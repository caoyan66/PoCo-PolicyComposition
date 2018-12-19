package edu.cseusf.poco.policy.staticAnalysis.scanPolicies;

import java.util.Arrays;

public class MtdArgs {
	private boolean  _resolvable;
	private Object[] _args;
	private String[] _argTyps;
	private boolean[] _isTrigEvt;
	
	public MtdArgs(Object[] args, String[] argTyps, boolean[] isTrig) {
		_args = args;
		_argTyps = argTyps;
		_isTrigEvt = isTrig;
		_resolvable = true;
	}
	public MtdArgs(Object[] args, String[] argTyps, boolean[] isTrig, boolean resolvable) {
		this(args, argTyps, isTrig);
		_resolvable = resolvable;
	}
	public MtdArgs() { this(null, null, null); }
	
	public Object[] getArgs() 	  { return _args; }
	public String[] getArgTyps()  { return _argTyps; }
	public boolean isResolvable() { return _resolvable; }
	public void setArgTyps(String[] argTyps) { _argTyps = argTyps; }
	public void setResolvable(boolean resolvable) { _resolvable = resolvable; }
	public boolean[] isTrigEvt() { return _isTrigEvt; }
	public void setArgs(Object[] args) 		 { _args = args; }
	public void setArg(Object arg, int index) {
		assert _args != null && index < _args.length;
		_args[index] = arg;
	}
	
	@Override
	public String toString() {
		return "MtdArgs [_resolvable=" + _resolvable + ", _args=" + Arrays.toString(_args) + ", _argTyps="
				+ Arrays.toString(_argTyps) + ", _isTrigEvt=" + Arrays.toString(_isTrigEvt) + "]";
	}
}