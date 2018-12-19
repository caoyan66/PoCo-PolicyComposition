package edu.cseusf.poco.event;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class EventUtil {
	public static String getMethodName(String methodStr) {
		if(!isMethod(methodStr)) {
			System.err.print("Invalid method!");
			System.exit(-1);
		}
		return getInfoFromMethodStr(methodStr,1);
	}
	
	public static String getArgStr(String methodStr) {
		if(!isMethod(methodStr)) {
			System.err.print("Invalid method!");
			System.exit(-1);
		}
		return getInfoFromMethodStr(methodStr,2).replaceAll(" ", "");
	}
	
	private static boolean isMethod(String methodStr) {
		String reg = "^(.+)\\((.*)\\)$";
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(methodStr);
		return matcher.find();
	}
	
	private static String getInfoFromMethodStr(String methodStr, int partIndex) {
		String reg = "^(.+)\\((.*)\\)$";
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(methodStr);
		
		if(matcher.find()){
		switch (partIndex) {
			//get method name only (not including the package name, return type, etc.)
			case 1:
				String temp = matcher.group(1);
				int length = temp.split("\\s+").length;
				if (length > 1) {
					return temp.split("\\s+")[length - 1];
				} else
					return temp;
				
			//get argument string
			case 2:
				return matcher.group(2);
				
			default: return methodStr;
			}
		}
		else
			return null;
	}
	/**
	 * trim the first six letters from the full class name (e.g., trim
	 * "class java.lang.reflect.Method" to "java.lang.reflect.Method")
	 * 
	 * @param fullClassName
	 * @return
	 */
	public static String trimClassName(String fullClassName) {
		if (fullClassName != null && fullClassName.length() >= 6)
			if (fullClassName.startsWith("class "))
				return fullClassName.substring(6, fullClassName.length());
		return fullClassName;
	}
	
	public static boolean sigMatch(String ptn, String val){
		String formatted = ptn.replace("(", "\\(").replace(")", "\\)")
				 .replace("[", "\\[").replace("]", "\\]")
				 .replace(".", "\\.").replace("*", ".+");
		Pattern pattern = Pattern.compile("^"+formatted+"$");
		Matcher matcher = pattern.matcher(val);
		if (matcher.find() )
			return true;
			
		String packageInfo = getPackageInfo(ptn);
		if(packageInfo.endsWith("+") ) {
			String ptnMtdName = getMtdName(ptn);
			String valMtdName = getMtdName(val);
			if(!ptnMtdName.equals(valMtdName))
				return false;
			
			packageInfo = packageInfo.substring(0, packageInfo.length()-1);
			String packageInfo4Val = getPackageInfo(val);
			try {
				Class<?> cls1 = Class.forName(packageInfo);
				Class<?> cls2 = Class.forName(packageInfo4Val);
				if (cls1.isAssignableFrom(cls2)) 
					return matchingArg(getArgstr(ptn), getArgstr(val));
			} catch (Exception e) {  return false; }
		}
		return false;
	}
	
	private static boolean matchingArg(String ptn, String val) {
		if(ptn.equals("*")) return true;
		if(ptn.equals("") && val.equals("")) return true;
		if(ptn.equals("") || val.equals("")) return false;
		
		String[] ptnargs = ptn.split(";");
		String[] valargs = val.split(";");
		if(ptnargs.length != valargs.length)
			return false;
		for(int i = 0; i<ptnargs.length; i++) {
			ptnargs[i] = ptnargs[i].replace("[", "\\[").replace("]", "\\]").replace(".", "\\.");
			Pattern pattern = Pattern.compile("^"+ptnargs[i]+"$");
			Matcher matcher = pattern.matcher(valargs[i]);
			if (!matcher.find()) return false;
		}
		return true;
	}

	private static String getPackageInfo(String sig) {
		String mtdName = sig.substring(0, sig.indexOf('('));
		if(mtdName.indexOf('.') != -1) 
			return mtdName.substring(0, mtdName.lastIndexOf('.'));
		return "";
	}
	
	private static String getMtdName(String sig) {
		String mtdName = sig.substring(0, sig.indexOf('('));
		if(mtdName.indexOf('.') != -1) 
			return mtdName.substring(mtdName.lastIndexOf('.')+1);
		return mtdName;
	}
	
	private static String getArgstr(String sig) {
		int left = sig.indexOf('(');
		int right = sig.indexOf(')');
		return right - left  == 1? "": sig.substring(left+1, right);
	}
	
	public static boolean argsMatch(Object[] _args, Object[] eArg) {
		if(_args == null)	return true;
		if( eArg == null)	return false;
		if(eArg.length != _args.length) return false;
		
		for(int i = 0; i< eArg.length; i++) {
			if(_args[i] == null && eArg[i] == null)
				continue;
			if(_args[i] == null || eArg[i] == null)
				return false;
			if(!eArg[i].equals(_args[i]))  return false;
		}
		return true;
	}
}