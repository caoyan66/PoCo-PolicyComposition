package edu.cseusf.poco.policy;

import edu.cseusf.poco.event.Event;

abstract public class Policy {
	private static boolean _outputNotSet;
	private static Event _outputEvt = null;

	/**
	 * this method responds to a trigger event, which is any security-relevant
	 * operation the system is monitoring
	 * 
	 * @param e the trigger event
	 */
	abstract public void onTrigger(Event e);

	/**
	 * a method that takes another policy's obligation, $o$, and returns a vote indicating approval or disapproval of $o$. This method is called {\tt vote}
	 * @param cfg 
	 * @return
	 */
	public boolean vote(CFG cfg) {
		return true;
	}

	public void onOblig(Rtrace rt) {
	}

	protected static void setOutput(Event e) {
		if (_outputNotSet) {
			_outputNotSet = false;
			_outputEvt = e;
		} else if (!_outputEvt.equals(e)) {
			// raise exception
			System.out.println("the trigger event: " + e.getEvtSig() + "'s output has been set as " + Policy.getOutput());
			System.out.println("the new output evt is: " + _outputEvt);
		}
	}

	public static boolean outputNotSet() {
		return _outputNotSet;
	}

	protected static void setOutputEvt(Event e) {
		_outputEvt = e;
	}

	static void resetOutputFlag(boolean val) {
		_outputNotSet = val;
	}

	public static Event getOutput() {
		return _outputEvt;
	}

	private CFG cfg4onTrigger;

	void setCfg4onTrigger(CFG cfg) {
		cfg4onTrigger = cfg;
	}

	CFG getCfg4onTrigger() {
		return cfg4onTrigger;
	}

	private CFG cfg4onOblig;

	void setCfg4onOblig(CFG cfg) {
		cfg4onOblig = cfg;
	}

	CFG getCfg4onOblig() {
		return cfg4onOblig;
	}
}