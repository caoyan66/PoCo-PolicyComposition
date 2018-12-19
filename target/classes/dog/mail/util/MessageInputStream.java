/*
 * MessageInputStream.java
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
 * A utility class for feeding message contents to messages.
 *
 * @author dog@dog.net.uk
 * @version 1.1
 */
public class MessageInputStream extends FilterInputStream {

	/**
	 * The stream termination octet.
	 */
	public static final int END = 46;
	
	/**
	 * The line termination octet.
	 */
	public static final int LF = 10;
	
	boolean done = false, debug = false;
	private boolean lf = false, lf_end = false;
	
	/**
	 * Constructs a message input stream connected to the specified pushback input stream.
	 */
	public MessageInputStream(PushbackInputStream in) {
		super(in);
	}

	/**
	 * Constructs a message input stream connected to the specified pushback input stream.
	 */
	public MessageInputStream(PushbackInputStream in, boolean debug) {
		super(in);
		this.debug = debug;
	}

	/**
	 * Reads the next byte of data from this message input stream.
	 * Returns -1 if the end of the message stream has been reached.
	 * @exception IOException if an I/O error occurs
	 */
	public int read() throws IOException {
		if (done) return -1;
		int ch = in.read();
		if (lf && lf_end) {
			int ch2 = in.read(); // look ahead for LF
			if (ch2==LF) {
				done = true;
				return -1; // swallow the END and LF
			} else {
				((PushbackInputStream)in).unread(ch2);
				lf = lf_end = false;
			}
		} else if (lf) {
			if (ch==END)
				lf_end = true;
		} else if (ch==LF) {
			lf = true;
		} else {
			lf = lf_end = false;
		}
		return ch;
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
		if (done) return -1;
		int l = doRead(b, off, len);
		int i = indexOfEnd(b, off, l);
		if (i>=0) {
			((PushbackInputStream)in).unread(b, i+off+2, (l-(i+2)));
			done = true;
			l = i;
		} else if ((l>0) && b[l-1]==LF) {
			lf = true;
			l--;
		} else if ((l>1) && (b[l-2]==LF && b[l-1]==END)) {
			lf_end = true;
			l-=2;
		}
		if (debug) {
			System.err.println("DEBUG: stream: read "+l+" bytes");
			System.err.println(new String(b, off, l));
		}
		return l;
	}
	
	// Prefixes a read with special LF or LF&END combinations if necessary
	private int doRead(byte[] b, int off, int len) throws IOException {
		int l = 0;
		if (lf_end) {
			b[off] = LF;
			b[off+1] = END;
			l += 2;
		} else if (lf) {
			b[off] = LF;
			l++;
		}
		l += in.read(b, off+l, len-l);
		lf_end = lf = false;
		return l;
	}
	
	/**
	 * Reads a line of input from this input stream.
	 */
	public String readLine() throws IOException {
		if (done)
			return null;
		StringBuffer buf = new StringBuffer();
		int l = 1024;
		byte[] b = new byte[l];
		l = doRead(b, 0, l);
		if (l<0)
			return null;
		while (l>-1 && !done) {
			int i = indexOfEnd(b, 0, l);
			if (b[0]==END && (b[1]==LF || b[1]==0)) {
				done = true;
				return null;
			} else if (b[0]==LF && b[1]==END && (b[2]==LF || b[2]==0)) {
				done = true;
				return null;
			} else if (i>0) {
				((PushbackInputStream)in).unread(b, i+2, (l-(i+2)));
				l = i;
			} else if ((l>0) && b[l-1]==LF) {
				lf = true;
				l--;
			} else if ((l>1) && (b[l-2]==LF && b[l-1]==END)) {
				lf_end = true;
				l-=2;
			}
			i = indexOfLF(b, 0, l);
			if (i>=0) {
				((PushbackInputStream)in).unread(b, i+1, l-i);
				buf.append(new String(b, 0, i));
				break;
			} else {
				buf.append(new String(b, 0, l));
			}
			l = doRead(b, 0, l);
		}
		if (debug) {
			System.err.println("DEBUG: stream: readLine: "+buf.toString());
		}
		return buf.toString();
	}
	
	// Discover the index of END in a byte array.
	int indexOfEnd(byte[] b, int off, int len) {
		for (int i=off+2; i<(off+len); i++)
			if (b[i]==LF && b[i-1]==END && b[i-2]==LF)
				return i-off-1;
		return -1;
	}
	
	// Discover the index of LF in a byte array.
	int indexOfLF(byte[] b, int off, int len) {
		for (int i=off; i<(off+len); i++)
			if (b[i]==LF)
				return i;
		return -1;
	}
	
}
