package edu.cseusf.poco.policy.examplePolicies;

import java.time.Duration;
import java.time.LocalDateTime;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;
import edu.cseusf.poco.policy.Rtrace;

/**
 * $P_{time}$ disallows a window pop-up unless a second has passed since the
 * last pop-up.
 * 
 * @author yan
 *
 */
public class P_time extends Policy {
	private static LocalDateTime lastPopTime = null;

	private final Action popupAction = new Action("javax.swing.JOptionPane.show*(*)");

	public void onTrigger(Event e) {
		Action popupAction = new Action("javax.swing.JOptionPane.show*(*)");
		if (e.matches(popupAction)) {
			if (lastPopTime != null || getDuration() < 1)
				setOutput(new Result(e, null));
		} else {
			if (e.isResult() && e.matches(popupAction))
				lastPopTime = LocalDateTime.now();
		}
	}

	public boolean vote(CFG cfg) {
		if (cfg.evtCounts(popupAction) == 0) {
			return true;
		} else {
			if (cfg.evtCounts(popupAction) == 1) {
				return (lastPopTime == null || getDuration() >= 1);
			}else {
				return false;
			}
		}
	}

	public void onOblig(Rtrace rt) { 
		if ( rt!= null && rt.locateEvts(popupAction) != null )
			lastPopTime = LocalDateTime.now();
	}
	
	private long getDuration() {
		Duration duration = Duration.between(lastPopTime, LocalDateTime.now());
		return duration.getSeconds();
	}
}