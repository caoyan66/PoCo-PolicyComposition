package edu.cseusf.poco.policy.staticAnalysis.scanPolicies;

public class EventInfo {
	private static final String FLAG4CFG = "FLAG";
	private String _sig;
	private MtdArgs _caller;
	private String _evtType; 
	private MtdArgs _args;
	private boolean _isResolvable;
	private String _property; //for storing expectiontype
	
	public EventInfo(String sig) {
		_sig = sig;
		if(ParsFlgConsts.IS_STATEMENT_FLAG(sig)) _evtType = FLAG4CFG;
	}
	public EventInfo(String sig, MtdArgs args, boolean resolvable, MtdArgs caller){ 
		this(sig); 
		_isResolvable = resolvable;
		_args = args;
		_caller = caller;
	}
	
	@Override
	public String toString() {
		return "EventInfo [_sig=" + _sig + ", _evtType=" + _evtType + ", _args=" + _args + ", _isResolvable="
				+ _isResolvable + ", _property=" + _property + "]";
	}
	public String getSig() 			{ return _sig; }
	public void setSig(String sig) 	{ _sig = sig; }
	public String getEvtType() 		{ return _evtType;}
	public MtdArgs getArgs() 		{ return _args; }
	public boolean isResolvable()	{ return _isResolvable;}
	public String getProperty() 	{ return _property; }
	public void setArgs(MtdArgs _args) 	 { this._args = _args; } 
	public void setEvtType(String evtType) 	 { _evtType = evtType;}
	public void setProperty(String property) {_property = property;}
	public MtdArgs getCaller() { return _caller;} 
}