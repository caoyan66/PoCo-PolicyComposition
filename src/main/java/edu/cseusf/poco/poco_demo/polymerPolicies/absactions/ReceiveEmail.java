package edu.cseusf.poco.poco_demo.polymerPolicies.absactions;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;

public class ReceiveEmail extends AbsAction {
	
	public boolean mapConc2Abs(Action conc) {
		if( conc.matches (new Action("javax.mail.Folder.getMessages()")) ||
			conc.matches (new Action("javax.mail.Folder.getMessage(int)")) ||
			conc.matches (new Action("com.sun.mail.imap.IMAPFolder.expunge()")) ||
			conc.matches (new Action("com.sun.mail.imap.IMAPFolder.fetch(javax.mail.Message[], *)")) ||
			conc.matches (new Action("com.sun.mail.imap.IMAPFolder.getMessae(int)")) ||
			conc.matches (new Action("com.sun.mail.imap.IMAPFolder.getMessageByUID(long)")) ||
			conc.matches (new Action("com.sun.mail.imap.IMAPFolder.getMessagesByUID(long[])")) ||
			conc.matches (new Action("com.sun.mail.imap.IMAPFolder.getMessagesByUID(long,long)")) ||
			conc.matches (new Action("com.sun.mail.imap.IMAPFolder.search(*)")) ||
			conc.matches (new Action("com.sun.mail.pop3.POP3Folder.expunge()")) ||
			conc.matches (new Action("com.sun.mail.pop3.POP3Folder.fetch(javax.mail.Message[], *)")) ||
			conc.matches (new Action("com.sun.mail.pop3.POP3Folder.getMessage(int)"))){
			
			return true;
		}
		return false;
	}
}