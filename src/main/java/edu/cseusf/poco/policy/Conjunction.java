package edu.cseusf.poco.policy;

import java.util.ArrayList;

public class Conjunction extends VC{
	public boolean evaluate(ArrayList<Boolean> votes) {
		for(Boolean vote: votes) 
			if(!vote) 
				return vote;  
		return true;
	}
}
