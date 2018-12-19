package edu.cseusf.poco.policy;

import java.util.ArrayList;

public class OrderAsListed extends OS{
	public ArrayList<CFG> priorize(ArrayList<CFG> obs) {
		return obs;
	}
}