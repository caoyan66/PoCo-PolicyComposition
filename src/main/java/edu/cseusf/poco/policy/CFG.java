package edu.cseusf.poco.policy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.EventUtil;
import edu.cseusf.poco.policy.staticAnalysis.scanPolicies.EventInfo;
import edu.cseusf.poco.policy.staticAnalysis.scanPolicies.MtdArgs;

public class CFG  {
	private List<CFG> _childnodes;
	private String _policyName;

	private boolean _isEvtResolvable;
	private EventInfo _event;

	//constructors
	CFG(String pName) {
		_policyName = pName;
		_childnodes = new ArrayList<>();
	}
	
	CFG(String pName, EventInfo evt) {
		this(pName);
		_event = evt;
		_isEvtResolvable = true;
		_event.setEvtType(evt.getEvtType());
	}
	CFG(String pName, EventInfo evt, boolean isResolvable) {
		this(pName, evt);
		_isEvtResolvable = isResolvable;
	}
	
	
	
	public boolean contains(Event e) {
		Queue<CFG> queue = new LinkedList<CFG>();
		queue.add(this);

		while (!queue.isEmpty()) {
			CFG node = queue.remove();
			if (node != null && !node.isFlagNode()) {
				if( !node._isEvtResolvable )
					continue;
				if( matches(e, node))
					return true;
			}
			addChildrentoQueue(queue,node);
		}
		return false;
	}

	private boolean matches(Event e, CFG node) {
		String sig = node.getEvent().getSig();
		if (EventUtil.sigMatch(e.getEvtSig(), sig)) {
			if (e.getArgs() == null)
				return true;
			else
				return EventUtil.argsMatch(e.getArgs(), node.getEvent().getArgs().getArgs());
		}
		return false;
	}

	public boolean containsIncludeUnreslv(Event e){
		Queue<CFG> queue = new LinkedList<CFG>();
		queue.add(this);

		while (!queue.isEmpty()) {
			CFG node = queue.remove();
			if (node != null && !node.isFlagNode()) {
				if( matchesIncludeUnreslv(e, node))
					return true;
			}
			addChildrentoQueue(queue,node);
		}
		return false;
	}

	private boolean matchesIncludeUnreslv(Event e, CFG node) {
		String sig = node.getEvent().getSig();

		if (EventUtil.sigMatch(e.getEvtSig(), sig)) {
			if (e.getArgs() == null)
				return true;
			else if(!node.getEvent().isResolvable() )
				return true;
			else
				EventUtil.argsMatch(e.getArgs(), node.getEvent().getArgs().getArgs());
		}
		return false;
	}

	public boolean containsUnresolved(Event e) {
		Queue<CFG> queue = new LinkedList<CFG>();
		queue.add(this);
		while (!queue.isEmpty()) {
            CFG node = queue.remove();
            if (node != null && !node.isFlagNode()) {
                String sig = node.getEvent().getSig();
                if (EventUtil.sigMatch(e.getEvtSig(), sig))
					if(!node.getEvent().isResolvable())
						return true;

			}
			addChildrentoQueue(queue,node);
		}
		return false;
	}

	public List<EventInfo> getConcernedEvts(Event e) {
        List<EventInfo> evts = new ArrayList<>();
		Queue<CFG> queue = new LinkedList<CFG>();
		queue.add(this);

		while (!queue.isEmpty()) {
			CFG node = queue.remove();
			if (!node.isFlagNode()) {
				String nodeSig = node.getEvent().getSig();
				if (EventUtil.sigMatch(e.getEvtSig(), nodeSig)) {
					if(nodeSig.contains(".")) {
						if( matches(e, node))
							evts.add(node.getEvent());
					} else {
						String mtdName = nodeSig.substring(0, nodeSig.indexOf('('));
						CFG cfg = Monitor.getPolicy2Mtd2CFG().get(_policyName).get(mtdName);
						evts.addAll(cfg.getConcernedEvts(e));
					}

				}
			}
			addChildrentoQueue(queue,node);
		}
		return evts;
	}
	
	private boolean checkOutput(CFG node, Event e) {
		MtdArgs mtdArgs = node.getEvent().getArgs();
		if (mtdArgs.isResolvable() && mtdArgs.getArgs()[0] != null) {
			//if it is trigger event then skip
			if(!mtdArgs.isTrigEvt()[0] && outputMatches(mtdArgs.getArgs()[0], e, mtdArgs.getArgTyps()[0])) 
				return true;
		}
		return false;
	}
	private boolean outputMatches(Object object, Event e, String typ) {
		switch (object.getClass().getName()) {
		case "Result":
		case "edu.cseusf.poco.event.Result":
			return false;

		case "Action":
		case "edu.cseusf.poco.event.Action":
			return ((Action) object).matches(e);

		case "edu.cseusf.poco.staticAnalysis.scanPolicies.MtdArgs":
			if (typ.equals("Action") || typ.equals("edu.cseusf.poco.event.Action")) {
				MtdArgs mtdArgs2 = (MtdArgs) object;
				if (mtdArgs2.isResolvable()) {
					Object[] actArgs = mtdArgs2.getArgs();
					if (actArgs != null && actArgs.length > 0) {
						// Action(String); Action(String,Object[])
						String arg0Type = mtdArgs2.getArgTyps()[0];
						if (arg0Type.equals("java.lang.String") && actArgs[0] != null) {
							Action act = new Action(actArgs[0].toString());
							if (actArgs.length == 2 && mtdArgs2.getArgTyps()[1].equals("Object[]")) {
								if (actArgs[1].getClass().getSimpleName().equals("MtdArgs"))
									act.setArg(1, ((MtdArgs) actArgs[1]).getArgs());
								else
									act.setArg(1, actArgs[1]);
							}
							return act.matches(e);
						}
					}
				}
			}
			return false;
		default:
			return false;
		}
	}
	
	private boolean matchAbsAct(Event e, CFG node, boolean isResolved) {
		AbsAction absAction = (AbsAction) e;
		Action act = new Action(node.getEvent().getSig());
		if (node.getEvent().isResolvable() && node.getEvent().getArgs() != null)
			act.setArgs(node.getEvent().getArgs().getArgs());
		
		boolean isMatch = absAction.mapConc2Abs(act);
		return isResolved ? node.getEvent().isResolvable() && isMatch
				          : !absAction.isResolvable();
	}

	private boolean matchConcAct(Event e, CFG node, boolean isResolved) {
		String sig = node.getEvent().getSig();

		if (EventUtil.sigMatch(e.getEvtSig(), sig)) {
			if(isResolved) {
				if (e.getArgs() == null)
					return true;
				else {
					if(!node.getEvent().isResolvable() || node.getEvent().getArgs() == null)
						return false;
					return EventUtil.argsMatch(e.getArgs(), node.getEvent().getArgs().getArgs());
				}
			}else {
				return !node.getEvent().isResolvable();
			}
		}
		
		return false;
	}
	
	void updateCFGwTrigger(Event e) {
		Queue<CFG> queue = new LinkedList<CFG>();
		queue.add(this);

		while (!queue.isEmpty()) {
			CFG node = queue.poll();
			if (!node.isRootNode() && !node.isEndOfMtdNode()) {
				//update caller if not null
				MtdArgs caller = node._event.getCaller();
				if(caller != null && caller.isTrigEvt()[0])  
					node._event.getCaller().setArg(e, 0);
				
				MtdArgs args = node._event.getArgs();
				if (args != null) {
					boolean[] isTrigEvt = args.isTrigEvt();
					for (int i = 0; i < isTrigEvt.length; i++)
						if (isTrigEvt[i]) {
							args.setArg(e, i);
						} else {
							Object evtArg = args.getArgs()[i];
							if (evtArg != null) {
								String typ = evtArg.getClass().getName();
								if (typ.equals("edu.cseusf.poco.staticAnalysis.scanPolicies.MtdArgs")) {
									MtdArgs evtArgs = (MtdArgs) evtArg;
									if (evtArgs != null) {
										boolean[] isTrigEvt2 = evtArgs.isTrigEvt();
										for (int j = 0; j < isTrigEvt.length; j++)
											if (isTrigEvt2[j])
												evtArgs.setArg(e, j);
									}
								}

							}
						}
				}
			}
			addChildrentoQueue(queue,node);
		}
	}

	public void printCFG_DFS() {
		EventInfo event = getEvent();
		List<CFG> children = getChildnodes();
		int size = (children == null) ? 0 : children.size();
		String sig = event.getSig();

		if (sig.equals("RootNode")) {
			System.out.println("Root -- has " + size + " children");
		} else if (sig.equals("END_OF_METHOD")) {
			System.out.println("END_OF_METHOD");
		} else {
			System.out.println(sig + " is resolvable = " + event.isResolvable() + "; -- has " + size + " children");
		}
		if (children != null)
			for (CFG child : children)
				child.printCFG_DFS();
	}

	public void printCFG_BFS() {
		Queue<CFG> queue = new LinkedList<CFG>();
		queue.add(this);
		while (!queue.isEmpty()) {
			CFG node = queue.remove();
			List<CFG> childNodes = node.getChildnodes();
			int childCount = (childNodes == null) ? 0 : childNodes.size();

			if (node.getEvent() == null)
				System.out.println("Root -- has " + childCount + " children");
			else {
				System.out.println("\t" + node.getEvent().getSig() + " -- has " + childCount + " children");
				MtdArgs mtdArgs = node.getEvent().getArgs();
				if (mtdArgs != null && mtdArgs.getArgs() != null) {
					Object[] objs = mtdArgs.getArgs();
					for (Object obj : objs) 
						if (obj != null)
							System.out.println("\t\t" + obj);
				}
			}
			if (childCount > 0)
				childNodes.forEach(child -> queue.add(child));
		}
	}
	

	private void addChildrentoQueue(Queue<CFG> queue, CFG node) {
		List<CFG> childNodes = node.getChildnodes();
		if (childNodes != null && childNodes.size() > 0)
			for (CFG child : childNodes) queue.add(child);
	}
	private boolean isFlagNode() 	  {	return isRootNode() || isEndOfMtdNode(); 		}
	private boolean isRootNode() 	  { return _event.getSig().equals("RootNode"); 		}
	private boolean isEndOfMtdNode()  { return _event.getSig().equals("END_OF_METHOD");	}
	private boolean isSetOutputNode() {
		return _event.getSig().equals("edu.cseusf.poco.policy.Policy.setOutput(Event)");
	}

	// getters and setters
	public List<CFG> getChildnodes() 		{ return _childnodes;		}
	String getPolicyName() 					{ return _policyName;		}
	void setPolicyName(String p) 			{ _policyName = p;			}
	public EventInfo getEvent() 			{ return _event;			}
	void setEvent(EventInfo event) 			{ _event = event;			}
	boolean isResolvable() 					{ return _isEvtResolvable;	}
	void setChildnodes(List<CFG> nodes) 	{ _childnodes = nodes;   }
	void setEvtResolvable(boolean isResolvable) { _isEvtResolvable = isResolvable;}
	public void addChildnode(CFG node) {
		if (!_childnodes.contains(node))
			_childnodes.add(node);
	}
	void addChildnodes(List<CFG> nodes) {
		if (nodes != null)
			for (CFG cfg : nodes)
				addChildnode(cfg);
	}
	void removeChildNode(CFG node) {
		if (_childnodes.contains(node))
			_childnodes.remove(node);
	}
}