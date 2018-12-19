package edu.cseusf.poco.policy.examplePolicies;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;
import edu.cseusf.poco.policy.examplePolicies.absaction.FileOpen;
import edu.cseusf.poco.policy.examplePolicies.absaction.OpenDangerousFile;

/**
 * The Pfile policy disallows users from opening the "secret.txt" file.
 * 
 * @author yan
 *
 */
public class P_file extends Policy {
	private final String dangerousFileName = "secret.txt";
	private Action openDangerousFile = new OpenDangerousFile(dangerousFileName);

	public void onTrigger(Event e) {
		if (e.matches(openDangerousFile))
			setOutput(new Result(e, null));
	}
	
	public boolean vote (CFG cfg) {
		return !cfg.contains(openDangerousFile) && 
			   !cfg.containsUnresolved(new FileOpen());
	}
}