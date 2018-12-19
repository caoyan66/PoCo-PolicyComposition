package edu.cseusf.poco.policy.examplePolicies;

import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;
import edu.cseusf.poco.policy.Rtrace;

/**
 * P_trivial is an empty policy; it allows all events to execute, without restriction
 * @author yan
 *
 */
public class P_trivial extends Policy{
	public void onTrigger(Event e) {}
	public boolean vote(CFG cfg) { return true; }
	public void onOblig(Rtrace rt) { }
}