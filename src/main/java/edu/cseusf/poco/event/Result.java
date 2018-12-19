package edu.cseusf.poco.event;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public class Result extends Event{
	protected Object _evtRes;
	private boolean _isconstructorOrVoid;
	boolean isIsconstructorOrVoid() { return _isconstructorOrVoid; }
	
	//constructors
	Result() {_evtTyp = EvtTyp.RESULT; _isconstructorOrVoid = false; _evtRes = null; }
	public Result(Event evt, Object val) { this(); _evtSig = evt.getEvtSig(); _args = evt.getArgs();_caller = evt.getCaller(); _evtRes = val;}
	public Result(Object evtRes) { this(); _evtRes = evtRes; }
	public Result(String sig, Object evtRes) { this(evtRes); _evtSig = sig; }
	public Result(JoinPoint joinPoint, Object ret) {
		super(joinPoint);
		_evtTyp = EvtTyp.RESULT;
		if(joinPoint.getKind().equals("constructor-call"))
			_isconstructorOrVoid = true; 
		else{
			Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
			Type type = method.getGenericReturnType();
			if(type.toString().equals("void"))
				_isconstructorOrVoid = true; 
			else
				_evtRes = ret;
		}
	}

	public boolean equals(Object obj) {
		if(obj == null) return false;
		Event e = (Event) obj;
		
		switch(e.getEventTyp()) {
		case RESULT:
			return EventUtil.sigMatch (e.getEvtSig(), _evtSig) && 
				   EventUtil.argsMatch(_args,   e.getArgs());
		default: 
			return false;
		}
	}
	 
	public Object getEvtRes() { return _evtRes; }
	public Result setRes(Object res) { _evtRes = res; return this; }
	public String toString() {
		return "Result [evtRes=" + _evtRes + ", evtTyp=" + _evtTyp + ", evtSig=" + _evtSig +  "]";
	}
}