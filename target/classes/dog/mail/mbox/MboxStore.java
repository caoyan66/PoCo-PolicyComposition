/*
 * MboxStore.java
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
import java.net.*;
import javax.mail.*;
import javax.mail.event.*;
import java.util.Hashtable;
import dog.mail.util.*;

/**
 * The storage class implementing the Mbox mailbox file format.
 *
 * @author dog@dog.net.uk
 * @version 1.3
 */
public class MboxStore extends Store {

	static int fetchsize = 1024;
	static boolean attemptFallback = false;
	Hashtable folders = new Hashtable();

	/**
	 * Constructor.
	 */
	public MboxStore(Session session, URLName urlname) {
		super(session, urlname);
		String ccs = session.getProperty("mail.mbox.fetchsize");
		if (ccs!=null) try { fetchsize = Math.max(Integer.parseInt(ccs), 1024); } catch (NumberFormatException e) {}
		String af = session.getProperty("mail.mbox.attemptFallback");
		if (af!=null) attemptFallback = Boolean.valueOf(af).booleanValue();
	}
	
	/**
	 * There isn't a protocol to implement, so this method just returns.
	 */
	protected boolean protocolConnect(String host, int port, String username, String password) throws MessagingException {
		return true;
	}

	/**
	 * Returns the default folder.
	 */
	public Folder getDefaultFolder() throws MessagingException {

		if (url!=null) {
			String file = url.getFile();
			if (file.length()>0) {
				String name = File.separator+file.replace('/', File.separatorChar);
				Folder folder = getFolder(name);
				return folder;
			} 
		} 
	    try {
		String defaultDir = session.getProperty("mail.mbox.userhome");
		if (defaultDir == null) {
		    defaultDir = System.getProperty("user.home");
		}
		return new MboxDefaultFolder(this, defaultDir);
	    } catch (SecurityException e) {
		throw new MessagingException("Access denied", e);
	    }
		    
		/*
		if (attemptFallback) {
			try {
				return getFolder(System.getProperty("user.home"));
			} catch (SecurityException e) {
				throw new MessagingException("Access denied", e);
			}
		}
		*/
		     
	}

	/**
	 * Returns the folder with the specified filename.
	 */
	public Folder getFolder(String filename) throws MessagingException {
		Folder folder = (Folder)folders.get(filename);
		if (folder==null) {
			if ("inbox".equals(filename.toLowerCase())) {
				// First try the session property mail.mbox.inbox.
				String m = session.getProperty("mail.mbox.inbox");
				if (m!=null && new File(m).exists())
					filename = m;
				else if (attemptFallback) { // If that fails try some common (UNIX) locations.
					try {
						m = File.separator+"var"+File.separator+"spool"+File.separator+"mail"+File.separator+System.getProperty("user.name");
						if (new File(m).exists())
							filename = m;
						else {
							m = System.getProperty("user.home")+File.separator+"mbox";
							if (new File(m).exists())
								filename = m;
						}
					} catch (SecurityException e) { // not allowed to read system properties
					}
				}
			}
			folders.put(filename, folder = new MboxFolder(this, filename));
		}
		return folder;
	}

	/**
	 * Returns the folder specified by the filename of the URLName.
	 */
	public Folder getFolder(URLName urlname) throws MessagingException {
        return getFolder(File.separator+urlname.getFile().replace('/', File.separatorChar));
	}
	
	Session getSession() {
		return session;
	}


}
