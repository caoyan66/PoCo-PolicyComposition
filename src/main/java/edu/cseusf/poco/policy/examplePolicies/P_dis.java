package edu.cseusf.poco.policy.examplePolicies;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;

/**
 * $P_dis(Event d)$ disallows an event passed as an argument to the policy.
 * @author yan
 *
 */
public class P_dis extends Policy {
	private Action disallowEvt;
	public P_dis(Action evt) { disallowEvt = evt;}
	
	public void onTrigger(Event e) {
		if( e.matches(disallowEvt) ) 
			setOutput(new Result(e, null));
	}
		
	public boolean vote (CFG cfg) {
		return !cfg.contains(disallowEvt) &&
			   !cfg.outputSets(disallowEvt);
	}
}