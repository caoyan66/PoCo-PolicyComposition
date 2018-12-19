package edu.cseusf.poco.policy;

import java.util.ArrayList;

/**
 * Vote Combinator
 * @author yan
 *
 */
public abstract class VC {
	abstract public boolean evaluate(ArrayList<Boolean> votes);
}