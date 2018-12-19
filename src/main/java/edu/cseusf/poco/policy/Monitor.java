package edu.cseusf.poco.policy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.policy.staticAnalysis.StaticAnalysis;

public class Monitor{
	private static String _jarFile;
	public static String getJarFile() { return _jarFile; }
	private VC _vc;
	private OS _os;
	private Policy[] _policies;
	public static int count = 0;
	
	private static Map<String, Map<String, CFG>> _policy2_Mtd2CFG;
	public static Map<String, Map<String, CFG>> getPolicy2Mtd2CFG() {return _policy2_Mtd2CFG;}
	private Map<CFG, Policy> _onTrig2Policy;
	
	private static final Lock _lock = new ReentrantLock();
	private static boolean _locked = false;
	private static Rtrace _rtrace;
	
	public Monitor(String jarFile, Policy[] policies, String path4SourceFiles, String path4AbsActfiles) {
		//the default vca is conjunction, and the default pca is orderAsListed
		 this(jarFile, policies, new Conjunction(),new OrderAsListed(), path4SourceFiles, path4AbsActfiles);
	}
	
	public Monitor(String jarFile, Policy[] policies, VC vca, OS pca, String path4SourceFiles, String path4AbsActfiles) {
		// 1. set vca and pca, 
		_jarFile = jarFile;
		_vc = vca;							
		_os = pca;
		
		// 2. set policies and compile
		_policies = policies; 
		compile(path4SourceFiles, path4AbsActfiles);
		
		// 3. get policy's ontrigger and onoblig CFGs
		_onTrig2Policy = new HashMap<CFG,Policy>();
		for(Policy p: _policies) {
			CFG onTrig = getCFG4OnTrig(p);
			p.setCfg4onTrigger(onTrig);
			_onTrig2Policy.put(onTrig, p);
			p.setCfg4onOblig(getCFG4OnOblig(p));
		}
	}
	
	public void processTrigger(Event e) {
		count++;
		// step 1: sorts policy's onTrigger obligations' CFGs
		ArrayList<CFG> sortedOnTrigger = _os.priorize(onTrigCFGs(_policies));
		
		// step 2: updates the sorted CFGs w/ the trigger event
		updateCFGwithTrigger(sortedOnTrigger, e);
		
		// step 3: reset the output flag
		Policy.resetOutputFlag(true);
		
		// step 4: adds sorted onTrigger to the stack of queue
		StackofQueue<Queue<Oblig2BExec>> soq = new StackofQueue<>();
		Queue<Oblig2BExec> queue = new LinkedList<>();
		
		for(CFG cfg: sortedOnTrigger) 
			queue.offer(new Oblig2BExec(_onTrig2Policy.get(cfg), e));
		soq.push(queue);
		
		// step 5: collects votes for each onTrigger obligation
		while ( !soq.isEmpty() ) {
			Oblig2BExec ob2BExec = soq.pop();
			synchronized(this) {
				ArrayList<Boolean> votes = collectVotes(ob2BExec,sortedOnTrigger);
				if(_vc.evaluate(votes)) {
					try {
						resetRtrace();
						if(_lock.tryLock()) {  _locked = true; ob2BExec.exec(); }
					} 
					finally { _lock.unlock(); _locked = false; }
				
					if(!_rtrace.isEmpty())  
						soq.push(add2ObligQueue(sortedOnTrigger));
				}
			}
		}
	}

	private ArrayList<CFG> onTrigCFGs(Policy[] ps) {
		ArrayList<CFG> onTrigCFGs = new ArrayList<CFG>();
		for(Policy p: ps)  
			onTrigCFGs.add(p.getCfg4onTrigger());
		return onTrigCFGs;
	}

	private Queue<Oblig2BExec> add2ObligQueue(ArrayList<CFG> cfgs) {
		Queue<Oblig2BExec> queue = new LinkedList<>();
		for(CFG cfg: cfgs)
			queue.offer(new Oblig2BExec(_onTrig2Policy.get(cfg), _rtrace));
		return queue;
	}
	
	private ArrayList<Boolean> collectVotes(Oblig2BExec ob2bExec, ArrayList<CFG> cfgs) {
		ArrayList<Boolean> votes = new ArrayList<Boolean>();
		Policy policy2BEvaluated = ob2bExec.policy;
		boolean isObOb = ob2bExec.isObligOnOblig;
		CFG cfg2BEvaluated = isObOb ? policy2BEvaluated.getCfg4onOblig()
				                    : policy2BEvaluated.getCfg4onTrigger();
		
		for(int i = 0; i<cfgs.size(); i++) {
			//TOCTOU
			Policy validatingPolicy = _onTrig2Policy.get(cfgs.get(i));
			votes.add(validatingPolicy.vote(cfg2BEvaluated));
		}
		return votes;
	}
	
	private void updateCFGwithTrigger(ArrayList<CFG> cfgs, Event trigger) {
		for(CFG cfg: cfgs)  
			cfg.updateCFGwTrigger(trigger);
	}
	
	private CFG getCFG4OnTrig(Policy p) 	{ return getCFG(p,"onTrigger");}
	private CFG getCFG4OnOblig(Policy p)   	{ return getCFG(p,"onOblig");  }
	private CFG getCFG(Policy p, String fname){ 
		return _policy2_Mtd2CFG.get(p.getClass().getName()).get(fname);
	}
	
	private void compile(String path4SourceFiles, String path4AbsActfiles) { 
		// step 1: validations 
		// ---- 1.1 at least one policy 
		validatePolicies();
		// ---- 1.2 path for source files is valid
		validateSourcePath(path4SourceFiles);
		if(path4AbsActfiles != null )
			validateSourcePath(path4AbsActfiles);
		
		//step 2. perform static analysis
		StaticAnalysis gen = new StaticAnalysis(_policies, path4SourceFiles, path4AbsActfiles);
		_policy2_Mtd2CFG   = gen.getPoicy2CFGs();
	}
	
	private void validateSourcePath(String path) {
		File file = new File(path);
		if ( !file.exists() || !file.isDirectory() )  
			msgThenExit("Cannot locate source code for Policies, please check the directory input!");
	}

	private void validatePolicies() {
		if(_policies == null  || _policies.length == 0) 
			msgThenExit("Fail to include any policy files, please check!");
	}
	
	private void msgThenExit(String msg){
		System.err.println(msg);
		System.exit(-1);
	}
	
	private void resetRtrace() { _rtrace = new Rtrace();}
	
	//getter and setter
	public Policy[] getPolicies() { return _policies; }
	public void setPolicies(Policy[] policies) { _policies = policies; }
	public OS getPCA() { return _os;}
	public void setPCA(OS pca) { _os = pca;}
	public VC getVCA() { return _vc; }
	public void setVCA(VC vca) { _vc = vca; }
	public Rtrace getRtrace() { return _rtrace; }
	public boolean isLocked4Oblig() { return _locked; }
	
	private class StackofQueue<T> {
		private Stack<Queue<Oblig2BExec>> stack;
		StackofQueue(){  stack = new Stack<>(); }
		boolean isEmpty() { return stack.isEmpty();}
		void push(Queue<Oblig2BExec> queue) { 
			if(queue != null && !queue.isEmpty())
				stack.push(queue);
		}
		Oblig2BExec pop() { 
			Queue<Oblig2BExec> queue =  stack.pop();
			Oblig2BExec ob2Exec = queue.poll();
			if(!queue.isEmpty())
				stack.push(queue);
			return ob2Exec;
		}
	}
	
	private class Oblig2BExec{
		Policy policy;			  
		boolean isObligOnOblig;	 
		Event trigger;
		Rtrace rtrace;

		Oblig2BExec(Policy p, Event e)   { 
			policy = p;	 trigger = e; isObligOnOblig = false;
		}
		Oblig2BExec(Policy p, Rtrace rt) { 
			policy = p;	 rtrace = rt; isObligOnOblig = true;
		}
		void exec() {
			if(isObligOnOblig)	policy.onOblig(rtrace);
			else 				policy.onTrigger(trigger);
		}
	}
}