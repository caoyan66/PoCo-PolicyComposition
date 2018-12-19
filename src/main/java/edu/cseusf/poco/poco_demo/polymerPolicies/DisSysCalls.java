package edu.cseusf.poco.poco_demo.polymerPolicies;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;

public class DisSysCalls extends Policy{
	Action sysCall = new Action("java.lang.Runtime.exec(*)");
	
	public void onTrigger(Event e){
		 if( e.isAction() && e.matches(sysCall) ) {
			 setOutput( new Result(e, null) );
		 }
	}
	public boolean vote(CFG cfg) { 
		return !cfg.contains(sysCall) && !cfg.outputSets(sysCall); 
	}
}
