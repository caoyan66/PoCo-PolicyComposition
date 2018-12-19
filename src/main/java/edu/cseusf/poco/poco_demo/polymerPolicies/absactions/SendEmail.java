package edu.cseusf.poco.poco_demo.polymerPolicies.absactions;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;

public class SendEmail extends AbsAction {
	public boolean mapConc2Abs(Action conc) {
		MimeMessage mime = null;
		
		if (conc.matches(new Action("javax.mail.Transport.send(javax.mail.Message)")) ||
			conc.matches(new Action("javax.mail.Transport.send(javax.mail.Message,javax.mail.Address[])")) ||
			conc.matches(new Action("javax.mail.Transport.sendMessage(javax.mail.Message,javax.mail.Address[])")) ||
			conc.matches(new Action("com.sun.mail.smtp.SMTPTransport.sendMessage(javax.mail.Message,*)")))  {
			
			mime = (MimeMessage) conc.getArg(0);
			StringBuilder confirmMsg = new StringBuilder("You are sending email with subject \n");
			
			try {
				String subj = mime.getSubject();
				if (subj == null) subj = "";
				if(subj.length() > 32) subj = subj.substring(0, 32);
				confirmMsg.append(subj + "..\nto the following address(es).\n");
						
				Address[] recips = mime.getAllRecipients();
				if(recips==null || recips.length==0) {
					confirmMsg.append("<could not find any recipients of message!>\n");
				}else {
					for(int j = 0; j<recips.length && j<20; j++) 
						confirmMsg.append("  " + recips[j].toString() + "\n");
			        if(recips.length >= 20)  confirmMsg.append("  ...\n");
				}
				confirmMsg.append("Select:\n  \"Yes\" to allow this mail to be sent,\n" + 
				                   "  \"No\" to halt the email client (without sending this mail), or\n"+
			                       "  \"Cancel\" to not send the currently outgoing mail but allow\n" + 
		                           "   the email client to continue running.");
				
				_matchingInfo = new Object[] { mime, confirmMsg.toString() };
				return true;
			} catch (Exception e) {  _resolvable = false; return true; }
		} 
		return false;
	}
}