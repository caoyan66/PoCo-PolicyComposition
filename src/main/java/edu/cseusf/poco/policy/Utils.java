package edu.cseusf.poco.policy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	public static String formatClassName(String str) {
		if (str == null)
			return null;
		if (str.startsWith("L"))
			str = str.substring(1);
		if (str.startsWith("ZL"))
			str = str.substring(2);
		if (str.endsWith(";"))
			str = str.substring(0, str.length() - 1);
		return str.replace('/', '.');
	}

	public static String formatArgList(String str) {
		// e.g., (Ljava/awt/Component;Ljava/lang/Object;Ljava/lang/String;I)I
		if (str == null)
			return null;
		int left = str.indexOf('(');
		int right = str.indexOf(')');
		if (left == -1 || right == -1 || left > right)
			return null;

		StringBuilder sb = new StringBuilder();
		String[] args = str.substring(left + 1, right).split(";");
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if(isArray(arg))
				arg = formatClassName(arg.substring(1))+ "[]";
			else
				arg = formatClassName(arg);
			if (!arg.equals("") ) {
				sb.append(handlePrimitiveType(arg)).append(",");
			}
		}
		if (sb.toString().equals(""))
			return null;
		else
			return sb.substring(0, sb.length() - 1);
	}

	private static boolean isArray(String str){
		if (str == null || str.length() == 0) return false;
		return str.startsWith("[");
	}
	private static String handlePrimitiveType(String str) {
		switch (str) {
		case "I":
			return "int";
		case "Z":
			return "boolean";
		default:
			return str;
		}
	}
	
	public static String getMtdClass(String str) {
		if (str == null) return null;
		String sig = getMtdNameWPackage(str);
		if(sig.lastIndexOf('.') != -1)
			return sig.substring(0, sig.lastIndexOf('.'));
		return sig;
	}

	public static String getSimpleMtdName(String str) {
		if (str == null)
			return null;
		 
		String longsig = getMtdNameWPackage(str);
		if(longsig.indexOf('.') != -1) 
			return longsig.substring(longsig.lastIndexOf('.')+1);
		else
			return longsig;
	}
	
	public static String getMtdName(String str) {
		if (str == null)
			return null;
		 
		return getMtdNameWPackage(str);
	}
 
	
	private static String getMtdNameWPackage(String str) {
		String reg = "^(.+)\\((.*)\\)$";
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(str);
		if(matcher.find())
			return matcher.group(1);
		else 
			return str;
	}
	
	public  static String[] getMethodParas(String str) {
		if(str == null) return null;
		
		String reg = "^(.+)\\((.*)\\)$";
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(str);
		if(matcher.find())
			return matcher.group(2).split(",");
		else 
			return null;
	}
	
	public static String validateStr(String str) {
		if (str == null)
			return null;

		return str.replace(".", "\\.").replace("(", "\\(").replace("$", "\\$")
				.replace(")", "\\)").replace("{", "\\{").replace("}", "\\}")
				.replace("#", "\\#").replace("@", "\\@").replace("?", "\\?")
				.replace("*", "(.*)");
	}
	
	public static boolean matchSignature(String sig, String str) {
		if(str == null || sig == null) return false;
		sig = sig.replace("(", "\\(").replace(")", "\\)")
				 .replace("[", "\\[").replace("]", "\\]")
				 .replace(".", "\\.").replace("*", ".+");
		Pattern pattern = Pattern.compile(sig);
		Matcher matcher = pattern.matcher(str);
		return matcher.find();
	}
	
	public static int isDefinedMethod(String str, String[] mtdName4Lists) {
		if(str == null || mtdName4Lists== null || mtdName4Lists.length==0) 
			return -1;
		str = getMtdName(str);
		if(!str.contains(".")) {
			for (int i = 0; i < mtdName4Lists.length; i++) 
				if(mtdName4Lists[i].equals(str)) return i;
		}
		return -1;
	}
}
