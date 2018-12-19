/*
 * MessageOutputStream.java
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
 * An output stream that escapes any dots on a line by themself with
 * another dot, for the purposes of sending messages to SMTP and NNTP servers.
 *
 * @author dog@dog.net.uk
 * @version 1.1
 */
public class MessageOutputStream extends FilterOutputStream {

	/**
	 * The stream termination octet.
	 */
	public static final int END = 46;
	
	/**
	 * The line termination octet.
	 */
	public static final int LF = 10;

	int[] last = { LF, LF }; // the last character written to the stream
	
	/**
	 * Constructs a message output stream connected to the specified output stream.
	 * @param out the target output stream
	 */
	public MessageOutputStream(OutputStream out) {
		super(out);
	}

	/**
	 * Writes a character to the underlying stream.
	 * @exception IOException if an I/O error occurred
	 */
	public void write(int ch) throws IOException {
		if (last[0]==LF && last[1]==END && ch==LF)
			out.write(END);
		out.write(ch);
		last[0] = last[1];
		last[1] = ch;
	}

	/**
	 * Writes a portion of a byte array to the underlying stream.
	 * @exception IOException if an I/O error occurred
	 */
	public void write(byte b[], int off, int len) throws IOException {
		for (int i = 0; i < len; i++) {
			int ch = (int)b[off+i];
			if (last[0]==LF && last[1]==END && ch==LF) {
				byte[] b2 = new byte[b.length+1];
				System.arraycopy(b, off, b2, off, i);
				b2[off+i] = END;
				System.arraycopy(b, off+i, b2, off+i+1, len-i);
				b = b2;
				i++; len++;
			}
			last[0] = last[1];
			last[1] = ch;
		}
		out.write(b, off, len);
	}
	
}
