package edu.cseusf.poco.poco_demo.polymerPolicies;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimePart;
import javax.swing.JOptionPane;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.GetMailContent;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.GetMailSubject;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.ReceiveEmail;
import edu.cseusf.poco.policy.Policy;

/**
 * This policy enforces the following: - Mail from unknown addresses has "SPAM?
 * - " prepended to subject (when message subject is queried at a high-level;
 * doesn't mess with messages' raw bytes) - "Really long" subjects are truncated
 * - Warn when receiving attachments on new mail from unknown addresses (prevent
 * warning overload by maintaining the stamped date of the last email we warned
 * against, not repeating warnings for mail stamped before that date), - All
 * mail retrieved is logged to file incoming.log (this file is created anew with
 * each run of the policy)
 */

public class IncomingEmail extends Policy {
	private static final String addrFilename = "/src/main/java/edu/cseusf/poco/poco_demo/known.addrs";
	private static final String logFilename = "/src/main/java/edu/cseusf/poco/poco_demo/incoming.log";
	private static Hashtable<String, LocalDateTime> warnedTable = new Hashtable<>();
	private static HashSet<String> loggedMsg = new HashSet<>();
	private Action getMailSubject, getMailContent, recvEmail; 
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, d MMM uuuu HH:mm:ss Z '('z')'");
	
	private PrintStream logFile;
	private final int MAX_SUBJ_LEN = 32;
	private ArrayList<String> trustedAddrs;
	private String msg = "This message contains an attachment that, if opened,"
			+ " could seriously harm\nyour computer.  Unless you specifically asked the sender for"
			+ " this attachment,\nit is strongly recommended that you delete this message immediately.";

	public IncomingEmail() {
		trustedAddrs = new ArrayList<>();
		File file = new File(Paths.get("").toAbsolutePath() + addrFilename);
		if (!file.exists() || file.isDirectory()) missingAddrBook();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			while (br.ready()) {
				String s = br.readLine();
				if (s != null && s.trim().equals("") == false) {
					String addr = s.trim().toLowerCase();
					trustedAddrs.add(addr.replace("@", "\\@").replace(".", "\\.").replace("*", ".+") );
				}
			}
			if (trustedAddrs.size() == 0) missingAddrBook();

			getMailSubject  = new GetMailSubject(trustedAddrs.get(0));
			getMailContent  = new GetMailContent(trustedAddrs.get(0));
			recvEmail =  new ReceiveEmail();
			File logfile = new File(Paths.get("").toAbsolutePath() + logFilename);
			logFile = new PrintStream(new BufferedOutputStream(new FileOutputStream(logfile, false)));

		} catch (Exception e) {
			System.err.println("Exception in IncomingMail policy: ");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void onTrigger(Event e) {
		if (e.isAction() && e.matches(getMailSubject)) {
			Message mime = (Message) e.getCaller();
			try {
				String subj = mime.getSubject();
				if (!isSenderKnown(mime)) 
					subj = "SPAM? - " + subj;
				if (subj.length() > MAX_SUBJ_LEN)
					subj = subj.substring(0, MAX_SUBJ_LEN);
				setOutput(new Result(e, subj));
			} catch (Exception ex) {}
		} 
		else if (e.isAction() && e.matches(getMailContent)) {
			boolean isknown = false;
			MimePart part= (MimePart) e.getCaller(); 
			try {
				String[] sa = part.getHeader("From");
			    if(sa!=null && sa.length >0) 
			    	isknown = checkSender(sa[0]);
			    if(!isknown && hasAttachment(part)) {
			    	String sender = part.getHeader("From")[0];
			    	sender = sender.toLowerCase();
					String[] dt = part.getHeader("Date");
					LocalDateTime ldt =  (dt != null && dt[0] != null) ? LocalDateTime.parse(dt[0], formatter)
																	   : LocalDateTime.now();
					
					ldt = ldt.truncatedTo(ChronoUnit.MINUTES);
					if (!warnedTable.containsKey(sender) || warnedTable.get(sender).isBefore(ldt)) {
						warnedTable.put(sender, ldt);
						JOptionPane.showMessageDialog(null, msg, "BEWARE", 0);
					}
			    }
			} catch (Exception e1) { }
		} 
		else if (e.isResult() && e.matches(recvEmail)) {
			logMailMsg(((Result) e).getEvtRes());
		}
	}

	private void logMailMsg(Object msgs) {
		if (msgs == null) 	return;
		switch (msgs.getClass().getName()) {
		case "javax.mail.Message":
		case "com.sun.mail.imap.IMAPMessage":
			logEmail((javax.mail.Message) msgs);
			return;
		default:return;
		}
	}
	
	private void logEmail(javax.mail.Message email) {
		if(email == null) return;
		try {
			Date date = email.getSentDate();
			Address from = email.getFrom()[0];
			String sub = email.getSubject();
			String id = from + sub;
			id = date +id;
		    if(id.equals("") || loggedMsg.contains(id))  return;
		    loggedMsg.add(id);
		    Enumeration<?> e = email.getAllHeaders();
		    logFile.println("-------------------------<PoCo NEXT MESSAGE>-------------------------------");
		    while(e.hasMoreElements()) {
		    	Header h = (Header)e.nextElement();
		        logFile.println(h.getName() + ": " + h.getValue());
		    }
		    Object content = email.getContent();
		    if(content instanceof String) {
		    }else if(content instanceof Multipart) {
		    	Multipart mmp = (Multipart)content;
		    	for(int j = 0; j<mmp.getCount(); j++) {
		    		BodyPart mbp = (BodyPart)(mmp.getBodyPart(j));
		            Object cont = mbp.getContent();
		            logFile.println("<PoCo>Multipart message, PART " + j + ":");
		            if(cont instanceof String) {
		                logFile.println("<PoCo:Type is " + mbp.getContentType() + ">");
		                logFile.println(cont);
		            }
		            else
		                logFile.println("<PoCo:Part not displayed; type is " + mbp.getContentType() + ">");
		            logFile.println("");
		          }
		       } else 
		    	   logFile.println("<PoCo>Unknown message type:\n" + content);
		}catch(Exception exn) {
		     logFile.println("<PoCo>There was an error opening the mail:\n" + exn);
		}
		logFile.flush();
	}

	private boolean hasAttachment(Part mm) {
		try {
			if (mm == null || mm.getContent() == null)
				return false;

			Object content = mm.getContent();

			if (content instanceof Multipart) {
				Multipart mp = (Multipart) content;
				for (int i = 0; i < mp.getCount(); i++)
					if (hasAttachment(mp.getBodyPart(i)))
						return true;
				return false;
			} else {
				if (mm.getContentType() == null)
					return true;
				
				String type = mm.getContentType();
				type = type.toUpperCase();
				if (type.startsWith("TEXT/PLAIN") || type.startsWith("TEXT/HTML") || type.startsWith("MESSAGE/RFC822"))
					return false;
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}

	private void missingAddrBook() {
		System.out.println("<IncomingMail> Error: you must have a file known.addrs");
		System.out.println("that contains your address book, one email address");
		System.out.println("per line, with your email address on the first line");
		System.exit(1);
	}
	
	private boolean isSenderKnown(Message mime) {
		try {
			String sender = mime.getHeader("From")[0];
			sender = sender.toLowerCase();
			return checkSender(sender);
		} catch (Exception e) { }
		return false;
	}
	private boolean checkSender(String addr) {
		for (int i = 0; i < trustedAddrs.size(); i++) {
			Pattern pattern = Pattern.compile(trustedAddrs.get(i));
			Matcher matcher = pattern.matcher(addr);
			if( matcher.find())
				return true;
		}
		return false;
	}
}