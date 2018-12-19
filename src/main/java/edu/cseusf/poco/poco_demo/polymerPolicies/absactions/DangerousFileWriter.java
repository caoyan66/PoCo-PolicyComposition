package edu.cseusf.poco.poco_demo.polymerPolicies.absactions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;

public class DangerousFileWriter extends AbsAction{
	private final List<String> dangerourExt;
	
	public DangerousFileWriter(String[] exts) {
		dangerourExt =  new ArrayList<String>( Arrays.asList(exts));
	}

	@Override
	public boolean mapConc2Abs(Action conc) {
		if ( conc.matches(new Action("java.io.File.createNewFile()")) || 
			 conc.matches(new Action("java.io.File.createTempFile(java.lang.String,java.lang.String,..)")) ||
			 conc.matches(new Action("java.io.File.renameTo(java.io.File)")) ||
			 conc.matches(new Action("java.io.FileOutputStream.<init>(java.io.File)")) ||
			 conc.matches(new Action("java.io.FileOutputStream.<init>(java.lang.String,..)")) ||
			 conc.matches(new Action("java.io.RandomAccessFile.<init>(java.lang.String,java.lang.String)")) ||
			 conc.matches(new Action("java.io.RandomAccessFile.<init>(java.io.File,java.lang.String)"))) {
			
			try{
				FileWrite fw = new FileWrite();
				if(fw.mapConc2Abs(conc) && fw.getEvtInfo() != null && fw.getEvtInfo()[0] != null) {
					String fName = fw.getEvtInfo()[0].toString();
					if( isDangerousFileWrite(fName) ) {
						_matchingInfo = fw.getEvtInfo();
						return true;
					} 
				}else  
					_resolvable = false;
			}catch(Exception ex) { _resolvable = false;}
		}
		return false;
	}

	private boolean isDangerousFileWrite(String fileName) {
		if(fileName.indexOf('.') != -1) {
			String fileExt = fileName.substring(fileName.lastIndexOf('.')+1);
			if(dangerourExt.contains(fileExt))
				return true;
		}
		return false;
	}
}