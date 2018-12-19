/*
 * MboxMessage.java
 * Copyright (C) 1999 dog <dog@dog.net.uk>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * You also have permission to link it with the Sun Microsystems, Inc. 
 * JavaMail(tm) extension and run that combination.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * You may retrieve the latest version of this library from
 * http://www.dog.net.uk/knife/
 */

package dog.mail.mbox;

import java.io.*;
import java.util.*;
import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * The message class implementing the Mbox mail protocol.
 *
 * @author dog@dog.net.uk
 * @version 1.3
 */
public class MboxMessage extends MimeMessage {

	/**
	 * The offset of the start of this message from the beginning of the file.
	 */
	protected long startOffset = -1;
	
	/**
	 * The offset of the start of this message's content from the beginning of the file.
	 */
	protected long contentOffset = -1;
	
	/**
	 * Creates a Mbox message.
	 * This is called by the MboxStore.
	 */
	protected MboxMessage(MboxFolder folder, InputStream in, int msgnum) throws MessagingException {
		super(folder, msgnum);
		if (!(in instanceof ByteArrayInputStream) && !(in instanceof BufferedInputStream))
			in = new BufferedInputStream(in);
		headers = new InternetHeaders(in);
		try {
			int fetchsize = MboxStore.fetchsize;
			byte bytes[];
			if (in instanceof ByteArrayInputStream) {
				fetchsize = in.available();
				bytes = new byte[fetchsize];
				int len = in.read(bytes, 0, fetchsize);
			} else {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				bytes = new byte[fetchsize];
				int len;
				while ((len = in.read(bytes, 0, fetchsize))!=-1)
					out.write(bytes, 0, len);
				bytes = out.toByteArray();
			}
			content = bytes;
		} catch(IOException e) {
			throw new MessagingException("I/O error", e);
		}
		readStatusHeader();
	}

	/**
	 * Creates a Mbox message.
	 * This is called by the MboxStore.
	 */
	protected MboxMessage(MboxFolder folder, RandomAccessFile file, int msgnum) throws MessagingException {
		super(folder, msgnum);
		// just create the headers for now
		headers = new InternetHeaders();
		try {
			startOffset = file.getFilePointer();
			String line;
			while ((line=file.readLine())!=null) {
				int len = line.length();
				if (len==0 || (len==1 && line.charAt(0)=='\r'))
					break;
				headers.addHeaderLine(line);
			}
			contentOffset = file.getFilePointer();
		} catch(IOException e) {
			throw new MessagingException("I/O error", e);
		}
		readStatusHeader();
	}
	
	/**
	 * Creates a Mbox message.
	 * This is called by the MboxFolder when appending.
	 * It creates a copy of the specified message for the new folder.
	 */
	protected MboxMessage(MboxFolder folder, MimeMessage message, int msgnum) throws MessagingException {
		super(folder, msgnum);
		headers = new InternetHeaders();
		for (Enumeration enum = message.getAllHeaderLines(); enum.hasMoreElements(); )
			headers.addHeaderLine((String)enum.nextElement());
		try {
			InputStream in = message.getInputStream();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] bytes = new byte[1024];
			for (int len = in.read(bytes); len>-1; len = in.read(bytes))
				out.write(bytes, 0, len);
			content = out.toByteArray();
		} catch (IOException e) {
			throw new MessagingException("I/O error", e);
		}
		readStatusHeader();
	}
	
	/**
	 * Returns the content of this message as a Java object.
	 */
	public Object getContent() throws MessagingException, IOException {
		if (content==null)
			retrieveContent();
		return super.getContent();
	}
	
	/**
	 * Returns the content of this message as a byte stream.
	 */
	public InputStream getContentStream() throws MessagingException {
		if (content==null)
			try {
				retrieveContent();
			} catch (IOException e) {
				throw new MessagingException("I/O error", e);
			}
		return super.getContentStream();
	}
	
	/**
	 * Returns the content of this message as a decoded stream.
	 */
	public InputStream getInputStream() throws MessagingException, IOException {
		if (content==null)
			retrieveContent();
		return super.getInputStream();
	}
	
	/**
	 * Returns the number of lines in the content of this message.
	 */
	public int getLineCount() throws MessagingException {
		if (content==null)
			try {
				retrieveContent();
			} catch (IOException e) {
				throw new MessagingException("I/O error", e);
			}
		return super.getLineCount();
	}
	
	protected void retrieveContent() throws IOException {
		if (contentOffset<0 || content!=null) return;
		int fetchsize = MboxStore.fetchsize;
		byte bytes[];
		RandomAccessFile file = 
			new RandomAccessFile(((MboxFolder)folder).file, "r");
		file.seek(contentOffset);
		String line;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (line = file.readLine(); line!=null; line = file.readLine()) {
			int fromIndex = line.indexOf("From ");
			if (fromIndex==0) { // line begins with From_, end of message
				content = out.toByteArray();
				break;
			} else {
				// strip quoting if necessary
				if (fromIndex>0) {
					String prefix = line.substring(0, fromIndex);
					boolean quoted = true;
					for (int i=0; i<prefix.length(); i++)
						if (prefix.charAt(i)!='>') {
							quoted = false;
							break;
						}
					if (quoted) {
						String suffix = line.substring(fromIndex);
						line = prefix.substring(1) + suffix;
					}
				}
				if (line.endsWith("\r"))
					line = line.substring(0, line.length()-1);
				out.write(line.getBytes());
				out.write('\n');
			}
		}
		if (line==null) // end of file
			content = out.toByteArray();
	}
		
	/**
	 * Returns the from address.
	 */
	public Address[] getFrom() throws MessagingException {
		Address[] a = getAddressHeader("From");
		if (a==null) a = getAddressHeader("Sender");
		return a;
	}

	/**
	 * Returns the recipients' addresses.
	 */
	public Address[] getRecipients(RecipientType type) throws MessagingException {
		if (type==RecipientType.NEWSGROUPS) {
			String key = getHeader("Newsgroups", ",");
			if (key==null) return null;
			return NewsAddress.parse(key);
		} else {
			return getAddressHeader(getHeaderKey(type));
		}
	}

	/**
	 * Returns the reply-to address.
	 */
	public Address[] getReplyTo() throws MessagingException {
		Address[] a = getAddressHeader("Reply-To");
		if (a==null) a = getFrom();
		return a;
	}

	/**
	 * Returns an array of addresses for the specified header key.
	 */
	protected Address[] getAddressHeader(String key) throws MessagingException {
		String header = getHeader(key, ",");
		if (header==null) return null;
		try {
			return InternetAddress.parse(header);
		} catch (AddressException e) {
            String message = e.getMessage();
			if (message!=null && message.indexOf("@domain")>-1)
				try {
					return parseAddress(header, "localhost");
				} catch (AddressException e2) {
					throw new MessagingException("Invalid address: "+header, e);
				}
			throw e;
		}
	}

	/**
	 * Makes a pass at parsing internet addresses.
	 */
	protected Address[] parseAddress(String in, String defhost) throws AddressException {
        Vector v = new Vector();
		for (StringTokenizer st = new StringTokenizer(in, ","); st.hasMoreTokens(); ) {
            String s = st.nextToken().trim();
			try {
				v.addElement(new InternetAddress(s));
			} catch (AddressException e) {
				int index = s.indexOf('>');
				if (index>-1) { // name <address>
					StringBuffer buffer = new StringBuffer();
					buffer.append(s.substring(0, index));
					buffer.append('@');
					buffer.append(defhost);
					buffer.append(s.substring(index));
					v.addElement(new InternetAddress(buffer.toString()));
				} else {
					index = s.indexOf(" (");
					if (index>-1) { // address (name)
						StringBuffer buffer = new StringBuffer();
						buffer.append(s.substring(0, index));
						buffer.append('@');
						buffer.append(defhost);
						buffer.append(s.substring(index));
						v.addElement(new InternetAddress(buffer.toString()));
					} else // address
						v.addElement(new InternetAddress(s+"@"+defhost));
				}

			}
		}
        Address[] a = new Address[v.size()]; v.copyInto(a);
		return a;
	}

	/**
	 * Returns the header key for the specified RecipientType.
	 */
	protected String getHeaderKey(RecipientType type) throws MessagingException {
		if (type==RecipientType.TO)
			return "To";
		if (type==RecipientType.CC)
			return "Cc";
		if (type==RecipientType.BCC)
			return "Bcc";
		if (type==RecipientType.NEWSGROUPS)
			return "Newsgroups";
		throw new MessagingException("Invalid recipient type: "+type);
	}

	// -- Need to override these since we are read-only --

	/**
	 * Mbox messages are read-only.
	 */
	public void setFrom(Address address) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void addFrom(Address aaddress[]) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void setRecipients(javax.mail.Message.RecipientType recipienttype, Address aaddress[]) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void addRecipients(javax.mail.Message.RecipientType recipienttype, Address aaddress[]) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void setReplyTo(Address aaddress[]) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void setSubject(String s, String s1) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void setSentDate(Date date) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void setDisposition(String s) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void setContentID(String s) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void setContentMD5(String s) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void setDescription(String s, String s1) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

	/**
	 * Mbox messages are read-only.
	 */
	public void setDataHandler(DataHandler datahandler) throws MessagingException {
		throw new IllegalWriteException("MboxMessage is read-only");
	}

        /** 
         * Ok, Mbox messages aren't entirely read-only.
         */
        public synchronized void setFlags(Flags flag, boolean set)
	    throws MessagingException {
	    if (set)
		flags.add(flag);
	    else
		flags.remove(flag);

	    updateStatusHeader();
	}
    
    /**
     * Updates the status header from the current flags.
     */
    private void updateStatusHeader() throws MessagingException {
	if (! flags.contains(Flags.Flag.SEEN)) {
	    this.setHeader("Status", "O");
	} else {
	    this.setHeader("Status", "RO");
	}
    }


    /**
     * Updates the status header from the current flags.
     */
    private void readStatusHeader() throws MessagingException {
	String[] currentStatus = this.getHeader("Status");
	if (currentStatus != null && currentStatus.length > 0) {
	    if (currentStatus[0].indexOf('R') >= 0)
		flags.add(Flags.Flag.SEEN);
	    if (currentStatus[0].indexOf('O') < 0)
		flags.add(Flags.Flag.RECENT);
	}

	updateStatusHeader();
    }

	// -- Utility methods --

	public boolean equals(Object other) {
		if (other instanceof MimeMessage) {
			MimeMessage message = (MimeMessage)other;
			return (message.getFolder()==getFolder() && message.getMessageNumber()==getMessageNumber());
		}
		return false;
	}

}
