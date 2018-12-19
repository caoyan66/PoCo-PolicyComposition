package edu.cseusf.poco.poco_demo.polymerPolicies.absactions;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;

public class LoadClasses extends AbsAction {

	public boolean mapConc2Abs(Action conc) {
		return conc.matches(new Action("java.lang.ClassLoader.<init>(*)"));
	}
	
	public boolean isDangerousClass() {
		try {
			StackTraceElement[] stEles = new Exception().getStackTrace();
			int i = 0;
			
			while (i < stEles.length - 1 && stEles[i].toString().startsWith("java.lang.ClassLoader.<init>") == false){
				System.out.println(stEles[i].toString());
				i++;
			}
			while (i < stEles.length - 1 && stEles[i].toString().indexOf("<init>") > 0) {
				System.out.println(i+": " + stEles[i].toString());
				if (stEles[i].toString().startsWith("java.") == false &&
					stEles[i].toString().startsWith("javax.") == false &&
					stEles[i].toString().startsWith("edu.cseusf.poco.") == false &&
					stEles[i].toString().startsWith("org.apache.bcel.") == false &&
					stEles[i].toString().startsWith("com.sun.") == false &&
					stEles[i].toString().startsWith("sun.") == false) {
					return true;
				} else
					i++;
			}
			if (i <= stEles.length - 1) 
				return true;
		} 
		catch (Exception ex) {return true; }
		
		return false;
	}
}