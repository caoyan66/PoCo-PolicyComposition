package edu.cseusf.poco.event;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;

public class Action extends Event {

	public static final Event SYSTEM_EXIT = null; // TODO: set null for now

	// constructors
	Action() { _evtTyp = EvtTyp.ACTION; }
	public Action(String actionSig) { this(); _evtSig = actionSig;}
	public Action(String actionSig, Object[] mtdArgs) { this(actionSig); _args = mtdArgs;}
	public Action(String actionSig, Object[] mtdArgs, Object caller) { this(actionSig, mtdArgs); _caller = caller;}
	public Action(JoinPoint joinPoint) {
		super(joinPoint);
		_evtTyp = EvtTyp.ACTION;
		_args = joinPoint.getArgs();
		_caller = joinPoint.getTarget();
	}

	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		Event e = (Event) obj;
		switch (e.getEventTyp()) {
		case ACTION:
			return EventUtil.sigMatch(e.getEvtSig(), _evtSig) && 
				   EventUtil.argsMatch(_args, e.getArgs());
		default:
			return false;
		}
	}

	public Object execute() {
		return Promoter.Reflect(this);
	}
	
	public String toString() {
		return "Action [_caller=" + _caller + ", _evtSig=" + _evtSig + ", _args=" + Arrays.toString(_args)
				+ ", _evtTyp=" + _evtTyp + ", _resolvable=" + _resolvable + "]";
	}
	// getter and setter
	void setCaller(Object caller) { _caller = caller;}
	public Object[] getEvtInfo() { return null;} 
}