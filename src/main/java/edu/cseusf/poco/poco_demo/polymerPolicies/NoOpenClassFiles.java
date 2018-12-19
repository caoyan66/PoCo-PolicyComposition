package edu.cseusf.poco.poco_demo.polymerPolicies;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.FileOpen;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;

/**
 * NoOpenClassFiles prevents the application from accessing compiled code that
 * could be executed by the Java runtime. This is done by prohibiting File
 * objects from being instantiated (via the special init method) on paths ending
 * in .class"
 * 
 * @author yan
 *
 */
public class NoOpenClassFiles extends Policy {
	private Action openClassFile = new FileOpen("*.class");

	public void onTrigger(Event e) {
		if(e.isAction() && e.matches(openClassFile) && outputNotSet())
			setOutput(new Result(e, null));
	}
	
	public boolean vote(CFG cfg) { 
		return !cfg.contains(openClassFile) && !cfg.outputSets(openClassFile); 
	} 
}