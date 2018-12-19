/*
 * CRLFInputStream.java
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

package dog.mail.util;

import java.io.*;

/**
 * An input stream that filters out CR/LF pairs into LFs.
 *
 * @author dog@dog.net.uk
 * @version 1.1
 */
public class CRLFInputStream extends PushbackInputStream {

	/**
	 * The CR octet.
	 */
	public static final int CR = 13;
	
	/**
	 * The LF octet.
	 */
	public static final int LF = 10;
	
	/**
	 * Constructs a CR/LF input stream connected to the specified input stream.
	 */
	public CRLFInputStream(InputStream in) {
		super(in, 4096);
	}

	/**
	 * Constructs a CR/LF input stream connected to the specified input stream, with the specified pushback buffer size.
	 */
	public CRLFInputStream(InputStream in, int bufsize) {
		super(in, bufsize);
	}

	/**
	 * Reads the next byte of data from this input stream.
	 * Returns -1 if the end of the stream has been reached.
	 * @exception IOException if an I/O error occurs
	 */
	public int read() throws IOException {
		int c = super.read();
		if (c==CR) // skip CR
			return super.read();
		return c;
	}

	/**
	 * Reads up to b.length bytes of data from this input stream into
	 * an array of bytes.
	 * Returns -1 if the end of the stream has been reached.
	 * @exception IOException if an I/O error occurs
	 */
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}
	
	/**
	 * Reads up to len bytes of data from this input stream into an
	 * array of bytes, starting at the specified offset.
	 * Returns -1 if the end of the stream has been reached.
	 * @exception IOException if an I/O error occurs
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		int l = doRead(b, off, len);
		l = removeCRs(b, off, l);
		//perr("read", new String(b, off, l));
		return l;
	}
	
	/*
	 * Slight modification of PushbackInputStream.read() 
	 * not to do a blocking read on the underlying stream if there are bytes in the buffer.
	 */
    private int doRead(byte[] b, int off, int len) throws IOException {
		if (len<=0)
		    return 0;
		int avail = buf.length-pos;
		if (avail>0) {
		    if (len<avail)
				avail = len;
		    System.arraycopy(buf, pos, b, off, avail);
		    pos += avail;
	    	off += avail;
		    len -= avail;
		} else if (len>0) {
		    len = super.read(b, off, len);
		    if (len==-1)
				return avail == 0 ? -1 : avail;
		    return avail + len;
		}
		return avail;
    }

	/**
	 * Reads a line of input terminated by LF.
	 * @exception IOException if an I/O error occurs
	 */
	public String readLine() throws IOException {
		StringBuffer sb = new StringBuffer();
		boolean done = false, eos = false;
		while (!done) {
			byte[] b = new byte[buf.length];
			int l = read(b);
			if (l==-1) {
				done = true;
				eos = true;
			} else {
				for (int i=0; i<l; i++) {
					if (b[i]==LF) {
						if (i>0)
							sb.append(new String(b, 0, i));
						if (l-(i+1)>0)
							unread(b, i+1, (l-i)-1);
						done = true;
						break;
					}
				}
				if (!done && l>0)
					sb.append(new String(b, 0, l));
			}
		}
		if (eos && sb.length()<1)
			return null;
		else
			return sb.toString();
	}

	private int removeCRs(byte[] b, int off, int len) {
		for (int index = indexOfCR(b, off, len); index>-1; index = indexOfCR(b, off, len)) {
			for (int i=index; i<b.length-1; i++)
				b[i] = b[i+1];
			len--;
		}
		return len;
	}

	private int indexOfCR(byte[] b, int off, int len) {
		for (int i=off; i<off+len; i++)
			if (b[i]==CR) return i;
		return -1;
	}

}
