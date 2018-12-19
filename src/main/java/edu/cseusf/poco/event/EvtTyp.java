package edu.cseusf.poco.event;

public enum EvtTyp {
	ABSACTION, ACTION, RESULT;

	public static boolean isAction(Event evt) {
		assert evt!= null;
		return evt.getEventTyp() == ACTION;
	}

	public static boolean isAbsAction(Event evt) {
		assert evt!= null;
		return evt.getEventTyp() == ABSACTION;
	}

	public static boolean isResult(Event evt) {
		assert evt!= null;
		return evt.getEventTyp() == RESULT;
	}
	 
}
