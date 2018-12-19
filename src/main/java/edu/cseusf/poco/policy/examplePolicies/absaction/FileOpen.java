package edu.cseusf.poco.policy.examplePolicies.absaction;

import java.io.File;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;


public class FileOpen extends AbsAction {
	@Override
	public boolean mapConc2Abs(Action conc) {
		String fName = null;
		
		if (conc.matches(new Action("java.io.FileInputStream.<init>(java.io.File)"))  			||
		    conc.matches(new Action("java.io.FileWriter.<init>(java.io.File)"))       			||
		    conc.matches(new Action("java.io.FileWriter.<init>(java.io.File,boolean)"))     	||
		    conc.matches(new Action("java.io.RandomAccessFile.<init>(java.io.File,java.lang.String)"))) {
			 
			try { fName = ((File) conc.getArg(0)).getName();
			 }catch(Exception ex) { _resolvable = false; }
		
		} else if (conc.matches(new Action("java.io.FileInputStream.<init>(java.lang.String)")) 	||
				 conc.matches(new Action("java.io.FileWriter.<init>(java.lang.String)"))       	||
				 conc.matches(new Action("java.io.FileWriter.<init>(java.lang.String,boolean)"))	||
			     conc.matches(new Action("java.io.RandomAccessFile.<init>(java.lang.String,java.lang.String)")) ||
				 conc.matches(new Action("java.util.zip.ZipFile.<init>(java.lang.String)"))) {
			
			try { fName = conc.getArg(0).toString();
			}catch(Exception ex) { _resolvable = false; }
		
		}
		
		if( fName != null ) {
			_matchingInfo = new Object[] {fName};
			return true;
		}
		return false;
	}

}