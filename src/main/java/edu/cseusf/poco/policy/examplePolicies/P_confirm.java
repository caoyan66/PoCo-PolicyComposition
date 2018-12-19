package edu.cseusf.poco.policy.examplePolicies;

import javax.swing.JOptionPane;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;
import edu.cseusf.poco.policy.examplePolicies.absaction.FileOpen;

/**
 * P_confirm requires every file-open operation attempted by the application
 * to first be confirmed, through a pop-up window, with the user.  
 * 
 * @author yan
 *
 */
public class P_confirm extends Policy {
	private String msg = "The program is attempting to open a file.\nAllow this operation?";
	private Action fopen = new FileOpen();
	
	public void onTrigger(Event e) {
		if( e.matches(fopen) && outputNotSet() ) {
    		if(JOptionPane.showConfirmDialog(null, msg, "Security Question", 0) == JOptionPane.YES_OPTION)
    			setOutput(e);
    		else 
    			setOutput(new Result(e,null));
    	}
	}
		
	public boolean vote (CFG cfg) { return !cfg.contains(fopen) && !cfg.outputSets(fopen); }
} 