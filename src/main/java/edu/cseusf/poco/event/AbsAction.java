package edu.cseusf.poco.event;

public abstract class AbsAction extends Action {
	protected Object[] _matchingInfo;
	
	public AbsAction() { this._evtTyp = EvtTyp.ABSACTION; this._resolvable = true;}
	
	public abstract boolean mapConc2Abs(Action evt);
	
	//getter and setter 
	public Object[] getEvtInfo() { return _matchingInfo; }
	 
}
