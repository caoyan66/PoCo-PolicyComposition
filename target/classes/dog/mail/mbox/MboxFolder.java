/*
 * MboxFolder.java
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
 *
 * Contributor(s): Daniel Thor Kristjan <danielk@cat.nyu.edu> close and expunge clarification.
 *                 Sverre Huseby <sverrehu@online.no> gzipped mailboxes
 */

package dog.mail.mbox;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import javax.mail.*;
import javax.mail.event.*;
import javax.mail.internet.*;
import dog.mail.util.*;

/**
 * The folder class implementing a UNIX mbox-format mailbox.
 *
 * @author dog@dog.net.uk
 * @version 1.3.3
 */
public class MboxFolder extends Folder {

	static final DateFormat df = new SimpleDateFormat("EEE MMM d H:m:s yyyy");
        static final String KNIFE_MESSAGE_ID = "X-Knife-Message-Id";
	
	File file;
	Vector messages;
	boolean open = false;
	int type = HOLDS_MESSAGES;
        boolean inbox = false;
        long fileLastModified = 0;
    
        /**
	 * Constructor.
	 */
        protected MboxFolder(Store store, String filename, boolean isInbox) {
	    super(store);
	    file = new File(filename);
	    if (file.exists() && file.isDirectory())
		type = HOLDS_FOLDERS;
	    inbox = isInbox;
	}

        /**
	 * Constructor.
	 */
        protected MboxFolder(Store store, String filename) {
	    this(store, filename, false);
	}

	/**
	 * Returns the name of this folder.
	 */
	public String getName() {
	    if ( ! inbox)
		return file.getName();
	    else
		return ("INBOX");
	}

	/**
	 * Returns the full name of this folder.
	 */
	public String getFullName() {
	    if (! inbox)
		return file.getAbsolutePath();
	    else
		return ("INBOX");
	}

	/**
	 * Returns the type of this folder.
	 * @exception MessagingException if a messaging error occurred
	 */
	public int getType() throws MessagingException {
		return type;
	}

	/**
	 * Indicates whether this folder exists.
	 * @exception MessagingException if a messaging error occurred
	 */
	public boolean exists() throws MessagingException {
		return file.exists();
	}

	/**
	 * Indicates whether this folder contains new messages.
	 * @exception MessagingException if a messaging error occurred
	 */
	public boolean hasNewMessages() throws MessagingException {
		return getNewMessageCount()>0;
	}

	/**
	 * Opens this folder.
	 * @exception MessagingException if a messaging error occurred
	 */
	public void open(int mode) throws MessagingException {
		switch (mode) {
		  case READ_WRITE:
			if (!file.canWrite())
				throw new MessagingException("Folder is read-only");
		  case READ_ONLY:
		}
		if (!file.canRead())
			throw new MessagingException("Can't read folder");
		try {
			if (((MboxStore)store).getSession().getDebug())
				System.err.println("DEBUG: mbox: opening "+file.getAbsolutePath());
			BufferedReader reader = new BufferedReader(new InputStreamReader(new CRLFInputStream(getInputStream())));
			String line = reader.readLine();
			if (line!=null && !line.startsWith("From "))
				throw new MessagingException("Mailbox format error", new ProtocolException());
		} catch (IOException e) {
			throw new MessagingException("Unable to open folder", e);
		}
		open = true;
		notifyConnectionListeners(ConnectionEvent.OPENED);
	}

	/**
	 * Closes this folder.
	 * @param expunge if the folder is to be expunged before it is closed
	 * @exception MessagingException if a messaging error occurred
	 */
	public void close(boolean expunge) throws MessagingException {
		if (open) {
			if (expunge) {
			    expunge();
			    open = false;
			    notifyConnectionListeners(ConnectionEvent.CLOSED);
			} else { 
			    open = false;
			    notifyConnectionListeners(ConnectionEvent.CLOSED);
			    saveMessages();
			}
		}
		if (((MboxStore)store).getSession().getDebug())
			System.err.println("DEBUG: mbox: closing "+file.getAbsolutePath());
	}
	
	/**
	 * Expunges this folder.
	 * This deletes all the messages marked as deleted.
	 * @exception MessagingException if a messaging error occurred
	 */
	public synchronized Message[] expunge() throws MessagingException {
		Vector ve = new Vector();
		if (open && messages!=null) {
			Vector vm = new Vector();
			for (Enumeration enum = messages.elements(); enum.hasMoreElements(); ) {
				Message message = (Message)enum.nextElement();
				Flags flags = message.getFlags();
				if (flags.contains(Flags.Flag.DELETED)) {
					ve.addElement(message);
				} else {
					vm.addElement(message);
				}
			}
			messages = vm;
		}
		Message[] expunged = new Message[ve.size()];
		ve.copyInto(expunged);
		if (expunged.length>0)
			notifyMessageRemovedListeners(true, expunged);
		return expunged;
	}
	
	/**
	 * Indicates whether this folder is open.
	 */
	public boolean isOpen() {
		return open;
	}

        public Flags permanentFlags = null;
	/**
	 * Returns the permanent flags for this folder.
	 */
	public Flags getPermanentFlags() { 
	    if (permanentFlags == null) {
		Flags tmpFlags = new Flags(); 
		tmpFlags.add(Flags.Flag.DELETED);
		tmpFlags.add(Flags.Flag.SEEN);
		tmpFlags.add(Flags.Flag.RECENT);
		permanentFlags = tmpFlags;
	    }

	    return permanentFlags;
	}
	
	/**
	 * Returns the number of messages in this folder.
	 * @exception MessagingException if a messaging error occurred
	 */
	public int getMessageCount() throws MessagingException {
		return getMessages().length;
	}

	/**
	 * Returns the specified message number from this folder.
	 * @exception MessagingException if a messaging error occurred
	 */
	public Message getMessage(int msgnum) throws MessagingException {
		try {
			return getMessages()[msgnum-1];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new MessagingException("No such message", e);
		}
	}

	/**
	 * Returns the messages in this folder.
	 * @exception MessagingException if a messaging error occurred
	 */
	public synchronized Message[] getMessages() throws MessagingException {
	    synchronizeMessages();
	    saveMessages();

	    Message[] m = new Message[messages.size()]; messages.copyInto(m);
	    return m;
	}

	// Reads messages from the disk file.
	private Vector readMessages() throws MessagingException {
		synchronized (this) {
			Vector tmpMessages = new Vector();
			int count = 0;
			try {
				RandomAccessFile raf = new RandomAccessFile(file, "r");
				String line;
				for (line = raf.readLine(); line!=null; line = raf.readLine()) {
					if (line.startsWith("From ")) { // new message
						Message message = new MboxMessage(this, raf, count++);
						tmpMessages.addElement(message);
					}
				}
				raf.close();
			} catch (IOException e) {
			    throw new MessagingException("I/O error reading mailbox", e);
			}
			return tmpMessages;
		}
	}

    /**
     * Synchronizes the source file with the current message list.
     */
    private void synchronizeMessages() throws MessagingException {
	if (file.lastModified() == fileLastModified)
	    return;

	Vector tmpMessages = readMessages();
	
	// we should never be in the position where we've removed messages
	// that haven't been removed from the file itself.  at least, let's
	// hope so.  :)

	// it should also be the case that messages are only appended to
	// the file, so if we find a message that doesn't correspond to a
	// current message, then it should be both a new message, and have
	// no old messages after it.

	// FIXME:  these are both really, really, really bad assumptions.

	if (messages == null)
	    messages = tmpMessages;
	else {
	    Vector messagesAdded = new Vector();
	    Vector finalMessages = new Vector();
	    
	    for (int i =0,j = -1; i < tmpMessages.size(); i++) {
		String tmpUniqueId = ((MboxMessage)tmpMessages.elementAt(i)).getMessageID();
		String uniqueId = null;

		while ( uniqueId != tmpUniqueId && ( !tmpUniqueId.equals(uniqueId)) && j < messages.size() - 1) { 
		    uniqueId = ((MboxMessage)messages.elementAt(++j)).getMessageID();
		}

		if (j < messages.size() -1) {
		    finalMessages.add(messages.elementAt(j));
		} else {
		    Object newMessage = tmpMessages.elementAt(i);
		    finalMessages.add(newMessage);
		    messagesAdded.add(newMessage);
		}
							    
	    }

	    messages = finalMessages;
	    if (messagesAdded.size() > 0) {
		Message[] n = new Message[messagesAdded.size()]; messagesAdded.copyInto(n);
		notifyMessageAddedListeners(n);
		
		saveMessages();
	    }
	}
    }
	
	// Saves messages to the disk file.
	private void saveMessages() throws MessagingException {
		synchronized (this) {
			if (messages!=null) {
				try {
					Message[] m = new Message[messages.size()];
					messages.copyInto(m);
					// make sure content has been retrieved for all messages
					for (int i=0; i<m.length; i++)
						if (m[i] instanceof MboxMessage)
							((MboxMessage)m[i]).retrieveContent();
				
					OutputStream os = new BufferedOutputStream(getOutputStream());
					MboxOutputStream mos = new MboxOutputStream(os);
					for (int i=0; i<m.length; i++) {
						Address[] f = m[i].getFrom();
						String from = "-";
						if (f.length>0) {
							if (f[0] instanceof InternetAddress)
								from = ((InternetAddress)f[0]).getAddress();
							else
								from = f[0].toString();
						}
						Date date = m[i].getSentDate();
						if (date==null)
							date = m[i].getReceivedDate();
						if (date==null)
							date = new Date();
					
						String top = "From "+from+ " "+df.format(date)+"\n";
						os.write(top.getBytes());
						m[i].writeTo(mos);
						mos.flush();
					}
					os.close();
					fileLastModified = file.lastModified();
				} catch (IOException e) {
					throw new MessagingException("I/O error writing mailbox", e);
				}
			}
		}
	}
	
	public synchronized void appendMessages(Message[] messages) throws MessagingException {
		synchronizeMessages();

		Vector added = new Vector();
		for (int i=0; i<messages.length; i++) {
			if (messages[i] instanceof MimeMessage) {
				MboxMessage message = new MboxMessage(this, (MimeMessage)messages[i], i);
				added.addElement(message);
				this.messages.addElement(message);
			}
		}
		if (added.size()>0) {
			Message[] n = new Message[added.size()]; added.copyInto(n);
			notifyMessageAddedListeners(n);
		}
		saveMessages();
	}

	/**
	 * Does nothing.
	 * The messages <i>must</i> be fetched in their entirety by getMessages() -
	 * this is the nature of the Mbox protocol.
	 * @exception MessagingException ignore
	 */
	public void fetch(Message amessage[], FetchProfile fetchprofile) throws MessagingException {
	}

	/**
	 * Returns the parent folder.
	 */
	public Folder getParent() throws MessagingException {
		return store.getFolder(file.getParent());
	}

	/**
	 * Returns the subfolders of this folder.
	 */
	public Folder[] list() throws MessagingException {
		if (type!=HOLDS_FOLDERS)
			throw new MessagingException("This folder can't contain subfolders");
		try {
			String[] files = file.list();
			Folder[] folders = new Folder[files.length];
			for (int i=0; i<files.length; i++)
				folders[i] = store.getFolder(file.getAbsolutePath()+File.separator+files[i]);
			return folders;
		} catch (SecurityException e) {
			throw new MessagingException("Access denied", e);
		}
	}

	/**
	 * Returns the subfolders of this folder matching the specified pattern.
	 */
    public Folder[] list(String pattern) throws MessagingException {
		if (type!=HOLDS_FOLDERS)
			throw new MessagingException("This folder can't contain subfolders");
		try {
			String[] files = file.list(new MboxFilenameFilter(pattern));
			Folder[] folders = new Folder[files.length];
			for (int i=0; i<files.length; i++)
				folders[i] = store.getFolder(file.getAbsolutePath()+File.separator+files[i]);
			return folders;
		} catch (SecurityException e) {
			throw new MessagingException("Access denied", e);
		}
	}

	/**
	 * Returns the separator character.
	 */
	public char getSeparator() throws MessagingException {
		return File.separatorChar;
	}

	/**
	 * Creates this folder in the store.
	 */
	public boolean create(int type) throws MessagingException {
		if (file.exists())
			throw new MessagingException("Folder already exists");
		switch (type) {
		  case HOLDS_FOLDERS:
			try {
				file.mkdirs();
				this.type = type;
				notifyFolderListeners(FolderEvent.CREATED);
				return true;
			} catch (SecurityException e) {
				throw new MessagingException("Access denied", e);
			}
		  case HOLDS_MESSAGES:
			try {
				// save the changes
				synchronized (this) {
                    if (messages==null) messages = new Vector();
					OutputStream os = new BufferedOutputStream(getOutputStream());
					Message[] m = new Message[messages.size()]; messages.copyInto(m);
					for (int i=0; i<m.length; i++) {
						Address[] f = m[i].getFrom();
						String top = "From "+((f.length>0) ? f[0].toString() : "-")+" "+df.format(m[i].getSentDate())+"\n";
						os.write(top.getBytes());
						m[i].writeTo(os);
					}
					os.close();
				}
				this.type = type;
				notifyFolderListeners(FolderEvent.CREATED);
				return true;
			} catch (IOException e) {
				throw new MessagingException("I/O error writing mailbox", e);
			} catch (SecurityException e) {
				throw new MessagingException("Access denied", e);
			}
		}
		return false;
	}

	/**
	 * Deletes this folder.
	 */
	public boolean delete(boolean recurse) throws MessagingException {
		if (recurse) {
			try {
				if (type==HOLDS_FOLDERS) {
					Folder[] folders = list();
					for (int i=0; i<folders.length; i++)
						if (!folders[i].delete(recurse))
							return false;
				}
				file.delete();
				notifyFolderListeners(FolderEvent.DELETED);
				return true;
			} catch (SecurityException e) {
				throw new MessagingException("Access denied", e);
			}
		} else {
			try {
				if (type==HOLDS_FOLDERS) {
					Folder[] folders = list();
					if (folders.length>0)
						return false;
				}
				file.delete();
				notifyFolderListeners(FolderEvent.DELETED);
				return true;
			} catch (SecurityException e) {
				throw new MessagingException("Access denied", e);
			}
		}
	}

	/**
	 * Mbox folders cannot be created, deleted, or renamed.
	 */
	public boolean renameTo(Folder folder) throws MessagingException {
		try {
            String filename = folder.getFullName();
			if (filename!=null) {
				file.renameTo(new File(filename));
				((MboxStore)store).folders.clear();
				notifyFolderListeners(FolderEvent.RENAMED);
				return true;
			} else
				throw new MessagingException("Illegal filename: null");
		} catch (SecurityException e) {
			throw new MessagingException("Access denied", e);
		}
	}

	/**
	 * Mbox folders cannot contain subfolders.
	 */
	public Folder getFolder(String filename) throws MessagingException {
		return store.getFolder(file.getAbsolutePath()+File.separator+filename);
	}

	/**
	 * Checks if the current file is or is supposed to be
	 * compressed. Uses the filename to figure it out.
	 */
	private boolean isGzip() {
		return file.getName().toLowerCase().endsWith(".gz");
	}

	/**
	 * Creates an output stream that possibly will compress
	 * whatever is sent to it, based on the current filename.
	 */
	private OutputStream getOutputStream() throws IOException {
		OutputStream out;

		out = new FileOutputStream(file);
		if (isGzip())
			out = new GZIPOutputStream(out);
		return out;
	}

    /** 
     * Locks this mailbox.  Not implementented yet.
     */
    public synchronized boolean acquireLock() {
	return true;
    }

    /**
     * Unlocks this mailbox.  Not implemented yet.
     */
    public synchronized boolean releaseLock() {
	return true;
    }

	/**
	 * Creates an input stream that possibly will decompress the
	 * file contents.
	 */
	private InputStream getInputStream() throws IOException {
		InputStream in;

		in = new FileInputStream(file);
		if (isGzip())
			in = new GZIPInputStream(in);
		return in;
	}

	class MboxFilenameFilter implements FilenameFilter {

		String pattern;
		int asteriskIndex, percentIndex;

		MboxFilenameFilter(String pattern) {
			this.pattern = pattern;
			asteriskIndex = pattern.indexOf('*');
			percentIndex = pattern.indexOf('%');
		}
		
		public boolean accept(File directory, String name) {
			if (asteriskIndex>-1) {
				String start = pattern.substring(0, asteriskIndex), end = pattern.substring(asteriskIndex+1, pattern.length());
				return (name.startsWith(start) && name.endsWith(end));
			} else if (percentIndex>-1) {
				String start = pattern.substring(0, percentIndex), end = pattern.substring(percentIndex+1, pattern.length());
				return (directory.equals(file) && name.startsWith(start) && name.endsWith(end));
			}
			return name.equals(pattern);
		}
		
	}

    
}
