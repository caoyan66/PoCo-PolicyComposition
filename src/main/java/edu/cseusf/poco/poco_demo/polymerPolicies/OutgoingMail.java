package edu.cseusf.poco.poco_demo.polymerPolicies;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Enumeration;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.swing.JOptionPane;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.SendEmail;
import edu.cseusf.poco.policy.Policy;

/**
 * Enforces policy on outgoing IMAP email. - popup a window listing recipients
 * of every email - log all outgoing email (append to outgoing.log file) -
 * automatically append textual contact info to text emails - automatically
 * backup all email by BCCing to home email address
 */

public class OutgoingMail extends Policy {
	private static final String logFilename = "/src/main/java/edu/cseusf/poco/poco_demo/outgoingMail.log";
	private InternetAddress backupAddr;
	private PrintStream logFile;
	private String contactInfo;

	public OutgoingMail() {
		try {
			contactInfo = "PoCo Demo\n University of South Florida";
			backupAddr = new InternetAddress("pocoDemo@gmail.com", true);
			File logfile = new File(Paths.get("").toAbsolutePath() + logFilename);
			logFile = new PrintStream(new BufferedOutputStream(new FileOutputStream(logfile, false)));
		} catch (Exception e) {
			System.err.println("Exception in OutgoingMail policy: ");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void onTrigger(Event e) {
		Action sendEmail = new SendEmail();

		if (e.isAction() && e.matches(sendEmail)) {
			if (!sendEmail.isResolvable()) {
				System.err.println("Exception in OutgoingMail policy!");
				System.exit(1);
			}

			Object[] emailInfo = sendEmail.getEvtInfo();
			MimeMessage mail = (MimeMessage) emailInfo[0];
			String msg = emailInfo[1].toString();
			int choice = JOptionPane.showConfirmDialog(null, msg, "Security Question",1);
			if (choice == JOptionPane.YES_OPTION) {
				try {
					mail.addRecipient(RecipientType.BCC, backupAddr);
					Object content = mail.getContent();
					
					if (content != null && !(content instanceof String)) {
						String typ = mail.getContentType();
						typ = typ.toLowerCase();
						if(typ.indexOf("text")>=0) {
							String str = (String) (content);
							mail.setContent(str + contactInfo, typ);
						}
					}
					e.setArg(0, mail);
					setOutput(e);
				} catch (Exception ex) {
					System.err.println("Exception in OutgoingMail policy: ");
					ex.printStackTrace();
					System.exit(1);
				}
			} else if (choice == JOptionPane.CANCEL_OPTION) {
				setOutput(new Result(e, null));
			} else { // choice == JOptionPane.NO_OPTION
				setOutput(new Result(null));
			}
		} else if (e.isResult() && e.matches(sendEmail)) {
			logMsg((MimeMessage) e.getArg(0));
		}
	}

	private void logMsg(MimeMessage mail) {
		logFile.println("-------------------------<NEXT MESSAGE>-------------------------------");
		try {
	         Enumeration<?> e = mail.getAllHeaders();
	         while(e.hasMoreElements()) {
	        	 Header h = ((Header)(e.nextElement()));
	        	 logFile.println(h.getName() + ": " + h.getValue());
	         }
	         Object content = mail.getContent();
	         if(content instanceof String) {
	        	 logFile.println(content);
	         }else if(content instanceof Multipart) {
	        	 Multipart mmp = (Multipart)content;
	        	 for(int j = 0; j<mmp.getCount(); j++) {
	        		 BodyPart mbp = (BodyPart)(mmp.getBodyPart(j));
	        		 Object cont = mbp.getContent();
	        		 logFile.println("Multipart message, PART " + j + ":");
	        		 if(cont instanceof String) {
	        			 logFile.println("<PoCo: Type is " + mbp.getContentType() + ">");
	        			 logFile.println(cont);
	        		 } else
	        			 logFile.println("<Part not displayed; type is " + mbp.getContentType() + ">");
	        		 logFile.println("");
	        	 }
	         } else 
	        	 logFile.println("<PoCo>Unknown message type:\n" + content);
	     }
	     catch(Exception exn) {
	         logFile.println("<PoCo>There was an error opening the mail:\n" + exn);
	     }
	     logFile.flush();
	}
}