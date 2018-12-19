/*
 * MboxDefaultFolder.java
 * Copyright (C) 2000 allen <allen@suberic.net>
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

import javax.mail.*;

/**
 * <p>
 * This represents a virtual root folder for the mbox JavaMail implementation.
 * It uses the session property 'mail.mbox.mailspool' as the spool directory
 * for the user's inbox, using the file with the user's username as the
 * inbox itself.  As an alternative, if you set the session property 
 * 'mail.mbox.inbox' to a specific file, then that file will be used as
 * the inbox.
 * </p>
 * 
 * <p>
 * In addition, by default this folder will return files in the user's 
 * home directory as available mail folders.  If the user wishes, she may
 * also set the 'mail.mbox.userhome' property to be this root directory, 
 * instead of the user's home.
 * </p>
 *
 * <p>
 * In other words, this is basically set up to emulate an IMAP server on
 * a local mail machine, so that clients that expect folders to be returned
 * using the IMAP paradigm can act that way.
 * </p>
 *
 * @author allen@suberic.net
 */
public class MboxDefaultFolder extends MboxFolder {

    /**
     * Creates the default Mbox folder.
     */
    protected MboxDefaultFolder(Store store, String filename) {
	super(store, filename);
    }


    /**
     * Returns the name of this folder.  In this case, returns "/".
     */
    public String getName() {
	return "/";
    }

    /**
     * Returns the full name of this folder.  In this case, returns "/".
     */
    public String getFullName() {
	return "/";
    }
    
    /**
     * Returns the subfolders of this folder.
     */
    public Folder[] list() throws MessagingException {

	Folder[] tmpValue = super.list();
	String inboxName = getInboxFilename();
	Folder inboxFolder = new MboxFolder(store, inboxName, true);
	Folder[] returnValue = new Folder[tmpValue.length  + 1];
	for (int i = 0; i < tmpValue.length; i++) {
	    returnValue[i] = tmpValue[i];
	}
	returnValue[tmpValue.length] = inboxFolder;
	return returnValue;
    }

    /**
     * Returns the subfolders of this folder matching the specified pattern.
     */
    public Folder[] list(String pattern) throws MessagingException {
	Folder[] tmpValue = super.list(pattern);
	MboxFilenameFilter filter = new MboxFilenameFilter(pattern);
	if (filter.accept(file, "INBOX")) {
	    String inboxName = getInboxFilename();
	    Folder inboxFolder = new MboxFolder(store, inboxName, true);
	    Folder[] returnValue = new Folder[tmpValue.length  + 1];
	    for (int i = 0; i < tmpValue.length; i++) {
		returnValue[i] = tmpValue[i];
	    }
	    returnValue[tmpValue.length] = inboxFolder;
	    return returnValue;
	} else 
	    return tmpValue;
    }


    /**
     * Gets the subfolder indicated by the given filename.  
     */
    public Folder getFolder(String filename) throws MessagingException {
	if (filename.equalsIgnoreCase("inbox")) {
	    String inboxName = getInboxFilename();
	    return new MboxFolder(store, inboxName, true);
	} else 
	    return store.getFolder(file.getAbsolutePath()+java.io.File.separator+filename);
    }

    /**
     * Deletes this folder.  Throws a MessagingException--let's not let
     * the user delete their home directory.  :)
     */
    public boolean delete(boolean recurse) throws MessagingException {
	throw new MessagingException("You cannot delete the root folder.");
    }

    /**
     * The root folder cannot be renamed.
     */
    public boolean renameTo(Folder folder) throws MessagingException {
	throw new MessagingException("You cannot rename the root folder.");
    }

    /**
     * Returns the inbox filename.
     */
    public String getInboxFilename() throws MessagingException {
	// thanks for putting getSession() there.  -akp
	Session session = ((MboxStore) store).getSession();
	String inboxName = session.getProperty("mail.mbox.inbox");
	if (inboxName != null)
	    return inboxName;

	String spoolFolder = session.getProperty("mail.mbox.mailspool");
	if (spoolFolder != null) {
	    try {
		inboxName = spoolFolder + getSeparator() + System.getProperty("user.name");
		return inboxName;
	    } catch (SecurityException se) {
		throw new MessagingException("Access denied.");
	    }
	}

	throw new MessagingException("Error:  neither mail.mbox.inbox nor mail.mbox.mailspool set.");
    }

}




