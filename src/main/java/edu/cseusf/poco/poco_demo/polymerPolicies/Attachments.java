package edu.cseusf.poco.poco_demo.polymerPolicies;

import javax.swing.JOptionPane;

import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.DangerousFileWriter;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;

/**
 * The Attachments policy intercepts actions that would create a file having a
 * forbidden extension. For the email client, this could happen if the user
 * downloads an attachment. The policy issues a warning via a pop up window,
 * where the user can choose to allow or cancel the download.
 * 
 * @author yan
 *
 */
public class Attachments extends Policy {
	private boolean userCancel = false, noAsk = false;
	private final String[] dangerourExt = new String[] {"exe", "vbs", "hta", "mdb","bad"};
	private DangerousFileWriter dangerousFW = new DangerousFileWriter(dangerourExt);
	
	public void onTrigger(Event e){
		if( e.isAction() && e.matches(dangerousFW) && outputNotSet() ) {
			
			if(noAsk) 		{ setOutput(e);	return;}
			if(userCancel) 	{ setOutput( new Result(e, null) ); return;}
        	
			Object info = dangerousFW.getEvtInfo()[0];
			String fn = info.toString();
        	String msg = "The target is creating a file named: "+ fn +".\nThis is a dangerous file type.  Are you sure you want to create this file?";
        	if(JOptionPane.showConfirmDialog(null, msg, "Security Question", 0) == JOptionPane.YES_OPTION) {
        		noAsk = true;
        		setOutput(e);
        	}else {
        		userCancel = true;
        		setOutput( new Result(e, null) );
        	}
        }
	}
	public boolean vote(CFG cfg) { 
		return !cfg.contains(dangerousFW) && 
			   !cfg.containsUnresolved(dangerousFW) && 
			   !cfg.outputSets(dangerousFW);
	}
}