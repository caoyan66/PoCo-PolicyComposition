package edu.cseusf.poco.policy.examplePolicies;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.policy.Policy;
import edu.cseusf.poco.policy.Rtrace;
import edu.cseusf.poco.policy.examplePolicies.absaction.FileOpen;

/**
 * Plog requires every file-open operation, including those attempted by the
 * applications and other policies, to be logged (except for the opening of the
 * log file).
*/
public class P_postlog extends Policy {
	private String logFileName;
	private Action fopen = new FileOpen();
	
	public P_postlog(String fileName) {
		this.logFileName = fileName;
		File f = new File(logFileName); 
		try { 
			if(!f.exists())
				f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("<P_log> Error: log file does not exist or cannot be created");
		} 
	}
	
	public void onTrigger(Event e) {
		if(e.isResult() && e.matches(fopen)) {
			Object name = e.getArg(0);
			log(name.toString());
		}
	}

	public void onOblig(Rtrace rt) {  
		if(rt != null) log(rt.getArgInfo(fopen, 0)); }
	
    private void log(String filesInfo) {
    	if(filesInfo == null) return;
    	try (FileWriter fWriter = new FileWriter(new File(logFileName), true)) {
			fWriter.write("The following file(s): \n\t"+ filesInfo +"\nhas been opened on" + LocalDateTime.now()+"\n\n");
			fWriter.flush();
		} catch (IOException e) { e.printStackTrace(); }
    }
    
}