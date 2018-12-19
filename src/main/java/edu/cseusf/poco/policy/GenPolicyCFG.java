package edu.cseusf.poco.policy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import edu.cseusf.poco.policy.staticAnalysis.scanPolicies.EventInfo;
import edu.cseusf.poco.policy.staticAnalysis.scanPolicies.ParsFlgConsts;
 
public class GenPolicyCFG {
	private static Stack<CFG> _stack4genCFG = new Stack<CFG>();
	private static CFG _currCFG;
	private static CFG _root;
	private static String _currPolicyName;
	private static String _currMtdName;
	
	public static CFG genCFG(ArrayList<EventInfo> evts, String policyName, String mtdName) {
		_stack4genCFG = new Stack<CFG>();
		init(policyName, mtdName);
		Set<CFG> flagNodes =new HashSet<CFG>();
		Set<CFG> pocoMtdNodes =new HashSet<CFG>();
		for(EventInfo ei: evts) {
			CFG cfg = genCFG(ei);
			add2CFG(cfg, flagNodes, pocoMtdNodes);
			_stack4genCFG.push(cfg);
		}
		add2CFG(new CFG(policyName, new EventInfo("END_OF_METHOD")));
		return _root;
	}
	
	private static CFG genCFG(EventInfo evt) {
		CFG cfg = new CFG(_currPolicyName, evt);
		if( !ParsFlgConsts.IS_STATEMENT_FLAG(evt.getSig()) )  
			cfg.setEvtResolvable(evt.isResolvable());
		
		return cfg;
	}
	
	private static void init(String policyName, String mtdName){
		_root = new CFG(policyName, new EventInfo("RootNode"));
		_currCFG = _root;
		_stack4genCFG = new Stack<CFG>();
		_currPolicyName = policyName;
		_currMtdName = mtdName;
	}
	
	private static void add2CFG(CFG cfg, Set<CFG> flagNodes, Set<CFG> pocoMtdNodes) {
		String sig = cfg.getEvent().getSig();
		
		if( ParsFlgConsts.IS_STATEMENT_FLAG(sig) ) 
			flagNodes.add(cfg);
		else {
			if(sig.startsWith("edu.cseusf.poco")) {
				if(!sig.equals("edu.cseusf.poco.policy.Policy.setOutput(Event)") && !sig.startsWith(_currPolicyName))  
					pocoMtdNodes.add(cfg);
			}
		}
		add2CFG(cfg);
	}

	private static void add2CFG(CFG cfg) {
		if (_currCFG.getEvent() == null || _stack4genCFG.size() == 0) {
			_currCFG.addChildnode(cfg);
		} else {
			switch (cfg.getEvent().getSig()) {
				case ParsFlgConsts.ELSE_BRANCH:	
					addElseBranching(cfg,0);	break;
				case ParsFlgConsts.ENDOFIF:		
					addEndIfNode(cfg,0);		break;
				case ParsFlgConsts.CATCH_BLOCK:	
					handleCatchNode(cfg);  		break;
				case ParsFlgConsts.ENDTRY:		
					handleEndTryNode(cfg);  	break;
				default: 						
					_currCFG.addChildnode(cfg); break;
			}
		}  
		_currCFG = cfg;
	}
	
	private static void handleEndTryNode(CFG cfg) {
		//step 1. locate matching try resources if exist 
		int index4TryResource = locateMatchingTryResource(0);
		int index4Catch = locateMatchingCatchBlock(0); 
		 
		//if exist, check throwable actions
		if(index4TryResource != -1) {
			for(int i = index4TryResource+1; i <_stack4genCFG.size(); i++) {
				String temp = _stack4genCFG.get(i).getEvent().getSig();
				if( isCloseableResource(temp) ) {
					CFG parent = _stack4genCFG.get(index4Catch-1);
					CFG catchNode = _stack4genCFG.get(index4Catch);
					if(!isThrowable(parent.getEvent().getSig(), catchNode.getEvent().getProperty())) {
						parent.removeChildNode(catchNode);
						//catchNode.removeParentNode(parent);
					}
					_currCFG = new CFG(_currPolicyName, new EventInfo(Utils.getMtdClass(temp) + ".close(null)"));
					parent.addChildnode(_currCFG);
					_currCFG.addChildnode(catchNode);
				}else if( ParsFlgConsts.IS_STATEMENT_FLAG(temp) )
					break;
			}
			_stack4genCFG.get(_stack4genCFG.size()-1).addChildnode(cfg);
			_currCFG.addChildnode(cfg);
		}
		else {
			// locate the matching catch_block flag, 
			// then add endTry node to the statement before the matching catch_block flag
			_stack4genCFG.get(index4Catch-1).addChildnode(cfg);
			_currCFG.addChildnode(cfg);
		}
	}

	private static boolean isCloseableResource(String sig) {
		if(sig==null) return false;
	    for(String flag: ParsFlgConsts.CLOSEABLE_RESOURCES) 
	    	if(Utils.matchSignature(Utils.validateStr(flag), sig) ) return true;
	    	
		return false;
	}

	private static void handleCatchNode(CFG cfg) {
		String expType = cfg.getEvent().getProperty();
		
		// step 1: locate the current catch statement's matching try-resource statement
		int index = locateMatchingTryResource(0); 
		//if exist, check throwable actions
		if(index != -1) {
			for(int i = index+1; i <_stack4genCFG.size(); i++) {
				String temp = _stack4genCFG.get(i).getEvent().getSig();
				if( isThrowable(temp, expType) ) 
					_stack4genCFG.get(i).addChildnode(cfg);
				else if( ParsFlgConsts.IS_STATEMENT_FLAG(temp) )
					break;
			}
		}
		
		// step 2: locate the current catch statement's matching try block statements  
		index = locateMatchingTryBlock(0); 
		// step 2.1 always should be able to locate the matching try block statements 
		if(index == -1) {
			System.err.println("Something went wrong when analyzing the " + _currMtdName + " method of the " + _currPolicyName + " policy!");
			System.exit(index);
		}
		// step 2.1 if there exists nested try-statements,  actions w/i those try-statements 
		// should be skipped for checking whether or not they are throwable
		for(int i = index+1; i <_stack4genCFG.size(); i++) {
			String temp = _stack4genCFG.get(i).getEvent().getSig();
			if( isThrowable(temp, expType) ) {
				_stack4genCFG.get(i).addChildnode(cfg);
			}else if( ParsFlgConsts.isTryStatement(temp) ) {
				//TODO: locate the matching end try and skip all actions w/i this try statement
				;
			}
		}
	}

	private static boolean isThrowable(String sig, String expType) {
		if(sig==null) return false;
	    switch (expType) {
			case "IOException": 
				return isIoThrowable(sig);
			default: 			
				return isIoThrowable(sig); //default for exception
	    }
	}

	private static boolean isIoThrowable(String sig) {
		for(String flag: ParsFlgConsts.THROWABLE_IO_EXCEPTION)  
	   		if( Utils.matchSignature(Utils.validateStr(flag), sig) ) return true;
		return false;
	}

	private static void addElseBranching(CFG cfg, int offset) {  
		int index = locateMatchingTHEN(offset); 
		_stack4genCFG.get(index-1).addChildnode(cfg);
	}
	
	private static void addEndIfNode(CFG cfg, int offset) {
		// step 1: always add to the last element on the stack 
		_stack4genCFG.get(_stack4genCFG.size()-1).addChildnode(cfg);
		//step 2: check whether the current if statement has else branch or not
		// if it has a else branch, then locate matching else on stack, then add cfg to the index-1;
		// otherwise, locate the matching then, then add add cfg to the index-1;
		int index4Else = locateMatchingELSE(offset); 
		if(index4Else != -1)  { 
			//if current if statement has has a else branch
			_stack4genCFG.get(index4Else-1).addChildnode(cfg);
		}else{
			int index4Then = locateMatchingTHEN(offset); 
			_stack4genCFG.get(index4Then-1).addChildnode(cfg);
		}
	}
	
	private static int locateMatchingTryResource(int offset) {
		return locateMatchingFlag(ParsFlgConsts.TRY_RESOURCE, offset, ParsFlgConsts.ENDTRY, ParsFlgConsts.TRY);
	}
	private static int locateMatchingTryBlock(int offset) {
		return locateMatchingFlag(ParsFlgConsts.TRY_BLOCK, offset, ParsFlgConsts.ENDTRY, ParsFlgConsts.TRY);
	}
	private static int locateMatchingCatchBlock(int offset) {
		return locateMatchingFlag(ParsFlgConsts.CATCH_BLOCK, offset, ParsFlgConsts.ENDTRY, ParsFlgConsts.TRY);
	}
	private static int locateMatchingTHEN(int offset) { 
		return locateMatchingFlag(ParsFlgConsts.THEN_BRANCH, offset, ParsFlgConsts.ENDOFIF, ParsFlgConsts.If_CONDITION);
	}
	private static int locateMatchingELSE(int offset) { 
		return locateMatchingFlag(ParsFlgConsts.ELSE_BRANCH, offset, ParsFlgConsts.ENDOFIF, ParsFlgConsts.If_CONDITION);
	}
	private static int locateMatchingFlag(String flag, int offset, String incFlag, String decFlag) {
		int count4End = 0;
		for (int i = offset; i < _stack4genCFG.size(); i++) {
			int index = _stack4genCFG.size() - 1 - i;
			String temp = _stack4genCFG.get(index).getEvent().getSig();
			if (temp.equals(flag) && count4End == 0) {
				return index;
			}else if (temp.equals(decFlag) ) {
				count4End--;
				if (count4End < 0 )
					return -1;
			} else if (temp.equals(incFlag))
				count4End++;
		}
		return -1;
	}
	
	@SuppressWarnings("unused")
	private static void printStack() {
		System.out.println("stack size: " + _stack4genCFG.size());
		for (int i = 0; i < _stack4genCFG.size(); i++) {
			CFG temp = _stack4genCFG.get(i);
			if (temp != null)
				System.out.println(i + "===" + temp.getEvent().getSig());
			else
				System.out.println("-- Root node");
		}
	}
}