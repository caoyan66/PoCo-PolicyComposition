package edu.cseusf.poco.policy.staticAnalysis.scanPolicies;

public class VariableObject {
	private String _vartype;
	private Object _varVal;
	private MtdArgs _varArgs;
	private boolean _isResolvable;

	public VariableObject(String vartype, Object varVal, boolean isResolvable) {
		setVartype(vartype);
		setVarVal(varVal);
		setResolvable(isResolvable);
	}

	public String getVartype() { return _vartype; }
	public Object getVarVal() { return _varVal; }
	public void setResolvable(boolean isResolvable) { _isResolvable = isResolvable; }
	public boolean isResolvable() { return _isResolvable; }
	public void setVarVal(Object varVal) { _varVal = varVal; }
	public void setVartype(String vartype) { _vartype = vartype; }
	public MtdArgs getVarArgs() { return _varArgs; }
	public void setVarArgs(MtdArgs varArgs) { _varArgs = varArgs;}
	@Override
	public String toString() {
		return "VariableObject [_vartype=" + _vartype + ", _varVal=" + _varVal + ", _varArgs=" + _varArgs
				+ ", _isResolvable=" + _isResolvable + "]";
	}
} 