package edu.cseusf.poco.policy;

import java.util.ArrayList;
/**
 * Prioritization Algorithm
 * @author yan
 *
 */
public abstract class OS {
	abstract public ArrayList<CFG> priorize(ArrayList<CFG> cfg);
}