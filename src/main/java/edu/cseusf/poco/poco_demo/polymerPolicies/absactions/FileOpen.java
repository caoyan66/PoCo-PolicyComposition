package edu.cseusf.poco.poco_demo.polymerPolicies.absactions;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;


public class FileOpen extends AbsAction {
	private String fileName;
	
	//constructors
	public FileOpen() { setFileName(".*"); /*any file name*/}
	public FileOpen(String fileName) { setFileName(fileName); }

	@Override
	public boolean mapConc2Abs(Action conc) {
		String fName = null;

		if (conc.matches(new Action("java.io.FileInputStream.new(File)"))  ||
		    conc.matches(new Action("java.io.FileWriter.new(File)"))       ||
		    conc.matches(new Action("java.io.RandomAccessFile.new(File, String)"))) {
			try{
				fName = ((File) conc.getArg(0)).getName();
			}catch(Exception ex) { _resolvable = false;}
			
		} else if (conc.matches(new Action("java.io.FileInputStream.new(String)")) ||
				   conc.matches(new Action("java.io.RandomAccessFile.new(String, String)"))) {

			String mode = conc.getArg(1).toString();
			if (mode.indexOf('r') >= 0 || mode.indexOf('R') >= 0)
				try{
					fName = conc.getArg(0).toString();
				}catch(Exception ex) { _resolvable = false;}

		} else if (conc.matches(new Action("java.util.zip.ZipFile.new(String)"))) {
			try{
				fName = conc.getArg(0).toString();
			}catch(Exception ex) { _resolvable = false;}
		}

		//it is resolvable
		if(fName == null) {
			_resolvable = false;
			return false;
		}else if (matchingFile(fName)) {
			_matchingInfo = new Object[] {fName};
			return true;
		}else
			return false;
	}

	private boolean matchingFile(String fName) {
		Pattern pattern = Pattern.compile(this.fileName);
		Matcher matcher = pattern.matcher(fName);
		return matcher.find();
	}
	
	public String getFileName() { return fileName; }
	public void setFileName(String fname) { 
		fileName = (fname == null || fname.length() == 0) ? ".*"
				                                          : fname.replace(".", "\\.").replace("*","(.*)");
	}
}
