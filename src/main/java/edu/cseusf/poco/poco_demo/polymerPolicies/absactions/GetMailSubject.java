package edu.cseusf.poco.poco_demo.polymerPolicies.absactions;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;

public class GetMailSubject extends AbsAction {
	private String selfMailAddr;
	
	public GetMailSubject(String addr) { selfMailAddr = addr; }

	public boolean mapConc2Abs(Action conc) {
		if (conc.matches(new Action("javax.mail.Message.getSubject()")) ||
		    conc.matches(new Action("javax.mail.internet.MimeMessage.getSubject()")) ||
			conc.matches(new Action("com.sun.mail.imap.IMAPMessage.getSubject()")) ){
			
			Message mime = (Message) conc.getCaller();
			return (isOutgoing(mime))? false: true;
		} 
		return false;
	}

	private boolean isOutgoing(Part p) {
		boolean isFrom = isSelf(p, true), isTo = isSelf(p, false);
		boolean ret = isFrom && isTo ? true : (isFrom && !isTo);
		return ret;
	}

	private boolean isSelf(Part p, boolean isFrom) {
		if (p == null)	return isFrom;

		try {
			String[] sa = isFrom ? p.getHeader("From") : p.getHeader("To");
			if (sa == null && p instanceof BodyPart) {
				Multipart mp = ((BodyPart) p).getParent();
				return (mp == null) ? isFrom : isSelf(mp.getParent(), isFrom);
			} else if (sa == null || sa.length == 0)
				return isFrom;

			for (int i = 0; i < sa.length; i++)
				if (sa[i].toLowerCase().indexOf(selfMailAddr) >= 0)
					return true;

			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}