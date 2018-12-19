package edu.cseusf.poco.event;

import org.aspectj.lang.JoinPoint;

public abstract class Event {
	protected Object _caller;
	protected String _evtSig;
	protected Object[] _args;
	protected EvtTyp _evtTyp;
	private String _property;
	
	protected boolean _resolvable = true;
	
	//constructors
	Event(){}
	public Event(JoinPoint joinPoint){
		//trim unnecessary informations, such as return type
		this._args = joinPoint.getArgs(); 
		String sigStr = joinPoint.getSignature().toString();
		String mtdName = EventUtil.getMethodName(sigStr);
		String argStr = EventUtil.getArgStr(joinPoint.getSignature().toLongString());
		if(joinPoint.getKind().equals("constructor-call")) 
			this._evtSig = mtdName + ".<init>(" +argStr+")";
		else
			this._evtSig = mtdName + "(" +argStr+")";
	}
	
	public boolean matches(Event e) {
		switch(e.getEventTyp()){
			case ABSACTION:	 
				return ((AbsAction)e).mapConc2Abs(new Action(_evtSig, _args, _caller));
			default:	
				return EventUtil.sigMatch(e.getEvtSig(), _evtSig) && 
					   EventUtil.argsMatch(e.getArgs(), _args);
		}
	}
	
	//setter and getter methods 
	public boolean isResolvable() { return _resolvable; }
	public String getEvtSig()         { return this._evtSig; }
	public void setEvtSig(String sig) { _evtSig = sig; }
	public EvtTyp getEventTyp()       { return _evtTyp; }
	public void setEventTyp(EvtTyp evtTyp) { _evtTyp = evtTyp; }
	public EvtTyp getEvtTyp() { return _evtTyp; }
	public Object[] getArgs() { return _args; }
	public Object getArg(int index) { if(_args != null && _args.length >index) return _args[index]; return null; }
	public void setArgs(Object[] arg)  {_args = arg;}
	public void setArg(int index, Object arg) { if(_args != null && _args.length >index) _args[index] = arg;}
	public Object getCaller() {return _caller;}
	void setResolvable(boolean resolvable) {_resolvable = resolvable; };
	String getProperty() { return _property; }
	void setProperty(String property) { _property = property; }
	public boolean isAction() { return _evtTyp.equals(EvtTyp.ACTION);}
	public boolean isResult() { return _evtTyp.equals(EvtTyp.RESULT);}
}