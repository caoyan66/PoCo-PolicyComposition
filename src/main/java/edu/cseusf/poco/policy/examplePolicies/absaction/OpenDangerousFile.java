package edu.cseusf.poco.policy.examplePolicies.absaction;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;

public class OpenDangerousFile extends AbsAction{
	private String _dangerousFileName;
	
	public OpenDangerousFile(String fname) {
		_dangerousFileName = fname;
	}

	@Override
	public boolean mapConc2Abs(Action conc) {
		if (conc.matches(new Action("java.io.FileInputStream.<init>(java.io.File)"))  		||
			conc.matches(new Action("java.io.FileWriter.<init>(java.lang.String)"))       	||
			conc.matches(new Action("java.io.FileWriter.<init>(java.lang.String,boolean)"))	||
			conc.matches(new Action("java.io.FileWriter.<init>(java.io.File)"))       		||
			conc.matches(new Action("java.io.FileWriter.<init>(java.io.File,boolean)"))      ||
			conc.matches(new Action("java.io.FileInputStream.<init>(java.lang.String)")) 	||
			conc.matches(new Action("java.io.RandomAccessFile.<init>(java.io.File,java.lang.String)")) ||
			conc.matches(new Action("java.io.RandomAccessFile.<init>(java.lang.String,java.lang.String)")) ||
			conc.matches(new Action("java.util.zip.ZipFile.<init>(java.lang.String)"))) {
			
			 AbsAction fopen = new FileOpen();
			 if( fopen.mapConc2Abs(conc) ) {
				 String fn = fopen.getEvtInfo()[0].toString();
				 return fn.equals(_dangerousFileName);
			 }else 
				_resolvable = fopen.isResolvable();
		}
		return false;
	}
}