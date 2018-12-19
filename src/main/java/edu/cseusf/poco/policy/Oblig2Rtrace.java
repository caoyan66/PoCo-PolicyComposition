package edu.cseusf.poco.policy;

import java.util.ArrayList;

public class Oblig2Rtrace {
	private ArrayList<Integer> _policyIndexes;
	private Rtrace _rt;
	
	public Oblig2Rtrace(ArrayList<Integer> indexes, Rtrace rt) {
		_policyIndexes = indexes;
		_rt = rt;
	}
	
	public Rtrace getRt() { return _rt; }
	public ArrayList<Integer> getPolicyIndexes() { return _policyIndexes; }
}
