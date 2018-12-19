/*
 * MboxOutputStream.java
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
 * Contributor(s): nil
 */

package dog.mail.mbox;

import java.io.*;

/**
 * A filter stream that can escape mbox From_ lines in message content.
 * This will only work reliably for messages with <1024 bytes in each line.
 * It will strip out any CRs in the stream.
 *
 * @author dog@dog.net.uk
 * @version 1.3
 */
class MboxOutputStream extends FilterOutputStream {
	
	private static byte KET = 62;
	
    /**
     * The buffer where the current line is stored. 
     */
    protected byte buf[];

    /**
     * The number of valid bytes in the buffer. 
     */
    protected int count = 0;
    
	public MboxOutputStream(OutputStream out) {
		super(out);
		buf = new byte[1024];
	}
	
    /** Flush the internal buffer */
    protected void validateAndFlushBuffer() throws IOException {
        if (count > 0) {
			for (int i=0; i<count-5; i++) {
				if (buf[i]=='F' && buf[i+1]=='r' && buf[i+2]=='o' && buf[i+3]=='m' && buf[i+4]==' ') {
					byte[] b2 = new byte[buf.length+1];
					System.arraycopy(buf, 0, b2, 0, buf.length);
					b2[i] = KET;
					System.arraycopy(buf, i, b2, i+1, buf.length-i);
					buf = b2;
					count++;
					break;
				} else if (buf[i]!=KET && buf[i]!='\n') {
					break;
				}
			}
			out.write(buf, 0, count);
			count = 0;
        }
    }

    /**
     * Writes the specified byte to this output stream. 
     */
    public synchronized void write(int b) throws IOException {
		if (b=='\r')
			return;
		if (b=='\n' || count>buf.length) {
			validateAndFlushBuffer();
		}
		buf[count++] = (byte)b;
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this output stream.
     */
    public synchronized void write(byte b[], int off, int len) throws IOException {
		// strip any CRs in the byte array
		for (int i=off; i<off+len; i++) {
			if (b[i]=='\r') {
				byte[] b2 = new byte[b.length];
				System.arraycopy(b, off, b2, off, len);
				System.arraycopy(b, i+1, b2, i, len-(i-off)-1);
				b = b2;
				len--;
				i--;
			}
		}
		// validate and flush a line at a time
		for (int i=off; i<off+len; i++) {
			if (b[i]=='\n' || i-off>buf.length) {
				int cl = (i-off>buf.length) ? buf.length : i-off;
				System.arraycopy(b, off, buf, count, cl);
				count += cl;
				validateAndFlushBuffer();
				len = len-(i-off);
				byte[] b2 = new byte[b.length];
				System.arraycopy(b, i, b2, off, len);
				b = b2;
				i = off;
			}
		}
		System.arraycopy(b, off, buf, count, len);
		count += len;
    }

    /**
     * Flushes this output stream.
     */
    public synchronized void flush() throws IOException {
        validateAndFlushBuffer();
		out.flush();
    }
	
}
