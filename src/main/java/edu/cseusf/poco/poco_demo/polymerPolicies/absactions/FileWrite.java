package edu.cseusf.poco.poco_demo.polymerPolicies.absactions;

import java.io.File;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;


public class FileWrite extends AbsAction {
	
	@Override
	public boolean mapConc2Abs(Action conc) {
		String fName = null;

		if ( conc.matches(new Action("java.io.File.createNewFile()")) ){
			try{
				fName = ((File)(conc.getCaller())).getName();
			}catch(Exception ex) { { _resolvable = false;}}
		} 
		else if ( conc.matches(new Action("java.io.File.createTempFile(java.lang.String,java.lang.String,..)")) ) {
			try {
				String arg1 = conc.getArg(0).toString();
				String arg2 = conc.getArg(1).toString();
				if(arg2.startsWith(".")) 
					fName = arg1 + arg2; 
				else 
					fName = arg1 + "."+ arg2;
			}catch(Exception ex) { _resolvable = false;}
		}
		else if (conc.matches(new Action("java.io.File.renameTo(java.io.File)")) ||
				 conc.matches(new Action("java.io.FileOutputStream.<init>(java.io.File)")) ) {
			try{
				fName = ((File)conc.getArg(0)).getName();
			}catch(Exception ex) { _resolvable = false;}
		}
		else if (conc.matches(new Action("java.io.FileOutputStream.<init>(java.lang.String,..)"))) {
			try {
				fName = conc.getArg(0).toString();
			}catch(Exception ex) { _resolvable = false;}
		}
		else if (conc.matches(new Action("java.io.RandomAccessFile.<init>(java.lang.String,java.lang.String)"))) {
			try {
				String mode = conc.getArg(1).toString();
				if(mode.indexOf('w')>=0 || mode.indexOf('W')>=0) 
					fName = conc.getArg(0).toString();
			}catch(Exception ex) { _resolvable = false;}
		}
		else if (conc.matches(new Action("java.io.RandomAccessFile.<init>(java.io.File,java.lang.String)"))) {
			try{
				String mode = conc.getArg(1).toString();
				if(mode.indexOf('w')>=0 || mode.indexOf('W')>=0) 
					fName = ((File)conc.getArg(0)).getName();
			}catch(Exception ex) { _resolvable = false;}
		}
		
		if( fName != null ) {
			_matchingInfo = new Object[] {fName};
			return true;
		}
		return false;
	}
}