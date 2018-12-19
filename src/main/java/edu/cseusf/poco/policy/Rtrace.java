package edu.cseusf.poco.policy;

import java.util.ArrayList;

import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.EventUtil;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.poco_demo.Abs2ConcreteAct;

public class Rtrace {
	private ArrayList<Result> _ress;
	
	public Rtrace() { _ress = new ArrayList<>(); }
	
	public boolean isEmpty() {  return _ress.size() == 0;}
	
	public boolean contains(Event e) {
		Event[] res = locateEvts(e);
		return res != null && res.length > 0;
	}
	
	public Event[] locateEvts(Event e) {
		if(_ress == null || _ress.size() ==0) return null;
		
		switch (e.getEvtTyp()) {
			case ABSACTION:	
				return handleAbsActs(e);
			default:		
				return handleConcActs(e);
		}
	}

	private Event[] handleAbsActs(Event e) {
		String[] sigs = Abs2ConcreteAct.getConcActs(e.getClass().getName());
		if(sigs == null || sigs.length ==0) return null;

		ArrayList<Event> evts = new ArrayList<>();
		for(Result res: _ress) {
			for(String sig: sigs) {
				if( EventUtil.sigMatch(sig, res.getEvtSig()) ) {
					evts.add(res);
					break;
				}
			}
		}
		return evts.size()>0 ? evts.toArray(new Event[evts.size()]) : null; 
	}
	
	private Event[] handleConcActs(Event e) {
		String matchSig = e.getEvtSig();
		Object[] args = e.getArgs();
		
		ArrayList<Event> evts = new ArrayList<>();
		for(Result res: _ress) {
			if( EventUtil.sigMatch(matchSig, res.getEvtSig()) ) {
				if(args == null)
					evts.add(res);
				else {
					if( EventUtil.argsMatch(res.getArgs(), args) )
						evts.add(res);
				}
			}
		}
		return evts.size()>0 ? evts.toArray(new Event[evts.size()]) : null; 
	}
	
	Object[] getEvtsArgInfo(Event e, int index) {
		Event[] evt = locateEvts(e);
		if(evt!=null && evt.length >0) {
			Object[] obj = new Object[evt.length];
			for(int i = 0; i<evt.length; i++) 
				obj[i] = index < evt[i].getArgs().length ? evt[i].getArg(index) : null;
			return obj;
		}
		return null;
	}
	
	public String getArgInfo(Event e, int index) {
		Object[] obj = getEvtsArgInfo(e, index);
		if(obj!=null && obj.length >0) {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < obj.length; i++) 
				if(obj[i] != null) 
					sb.append(obj[i].toString() + ";");
			return sb.substring(0, sb.length()-1);	
		}
		return null;
	}

	// getter and setter
	public void addRes(Result evt) { _ress.add(evt); }
	ArrayList<Result> getRess() { return _ress; }
}
