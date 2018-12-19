package edu.cseusf.poco.poco_demo.polymerPolicies;

import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.LoadClasses;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;

public class ClassLoaders extends Policy {
	private LoadClasses loaderClass = new LoadClasses();
	
	public void onTrigger(Event e){
		if ( e.isAction() && e.matches(loaderClass) )
			if(outputNotSet() && loaderClass.isDangerousClass())
				setOutput(new Result(e, null));
	}
	public boolean vote(CFG cfg) { 
		return  !cfg.contains(loaderClass) && !cfg.outputSets(loaderClass);
	}
}