package edu.cseusf.poco.poco_demo.polymerPolicies;

import java.lang.reflect.Method;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;
import edu.cseusf.poco.policy.Rtrace;

public class Reflection extends Policy {
	public void onTrigger(Event e) {
		if( e.isAction() && 
			e.matches(new Action("java.lang.reflect.Method.invoke(java.lang.Object,java.lang.Object[])")) ) {
			 Action act = (Action) e;
			 Method mtd = (Method)(act.getCaller());
			 String pkg = mtd.getDeclaringClass().getName();
	         if (pkg.startsWith("edu.cseusf.poco")) 
	        	 setOutput( new Result(null));
		}
	}
	public boolean vote(CFG cfg) { return true; }
	public void onOblig(Rtrace rt) { }
}
