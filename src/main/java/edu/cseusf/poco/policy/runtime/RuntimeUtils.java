package edu.cseusf.poco.policy.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuntimeUtils {
    private static final String reg = "^(.+)\\((.*)\\)$";
    private static final String regwPackage = "^(.+\\.)*(.+)$";

    public static String getMtdName(String sig) {
        String mtdNameWpkg = getMtdPackInfo(sig);
        if(mtdNameWpkg != null) {
            Pattern pattern = Pattern.compile(regwPackage);
            Matcher matcher = pattern.matcher(mtdNameWpkg);
            return matcher.find() ? matcher.group(2) : mtdNameWpkg;
        }
        return mtdNameWpkg;
    }
    public static String getPackageName(String sig) {
        String mtdNameWpkg = getMtdPackInfo(sig);
        if(mtdNameWpkg != null) {
            Pattern pattern = Pattern.compile(regwPackage);
            Matcher matcher = pattern.matcher(mtdNameWpkg);
            if (matcher.find()) {
                String pkgName = matcher.group(1);
                if(pkgName != null)
                    return pkgName.endsWith(".") ? pkgName.substring(0, pkgName.length() - 1) : pkgName;
            }
        }
        return null;
    }
    private static String getMtdPackInfo(String sig) {
        assert  sig != null && sig.length()>0;
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(sig);
        return matcher.find() ? matcher.group(1) : null;
    }

	static String getMethodName1(String funStr)  { return getMethodInfo(funStr, 0); }
	static String getMethodInfos(String funStr) { return getMethodInfo(funStr, 1); }
	public static String getMethodNamewoPackageInfo(String funStr) {
		String name = getMethodInfo(funStr, 0);
		if(name != null && name.length() > 0 && name.indexOf('.') > 0)
			name = name.substring(name.lastIndexOf('.') + 1);
		return name;
	}
	public static String getfunArgstr(String funStr) {
		if (funStr == null)
			return "";

		String returnStr = getMethodInfo(funStr, 2);
		// delete empty spaces between all parameters
		// joinPoint.getSignature().toLongString() will insert an empty
		// space between each parameter
		// such as java.io.FileWriter.new(java.lang.String, boolean)
		if (returnStr == null || returnStr.trim().length() == 0)
			return "";

		String[] args = returnStr.split(",");
		if (args.length > 1) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				sb.append(args[i].trim());
				if (i != args.length - 1)
					sb.append(",");
			}
			returnStr = sb.toString();
		}
		return returnStr;
	}
	
	/**
	 * This function get the requested information of a function
	 * 
	 * @param functionStr
	 * @param mode
	 *            0: return only methodName info; 1: return method Name along
	 *            with other information, such as Access Modifiers
	 * @return
	 */
	private static String getMethodInfo(String functionStr, int mode) {
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(functionStr);
		if (matcher.find()) {
			int lParen = functionStr.indexOf('(');
			int rParen = functionStr.lastIndexOf(')');
			switch (mode) {
			// 0: return only methodName info
			case 0:
				String temp = functionStr.substring(0, lParen);
				int length = temp.split("\\s+").length;
				if (length > 1) {
					return temp.split("\\s+")[length - 1];
				} else
					return temp;
				// 1: return method Name along with other information, such as
				// Access Modifiers
			case 1:
				return functionStr.substring(0, lParen);
			default: // mode = 2, return argstr
				return functionStr.substring(lParen + 1, rParen);
			}
		} else {
			switch (mode) {
			// 0: return only methodName info
			case 0:
				int length = functionStr.split("\\s+").length;
				if (length > 1)
					return functionStr.trim().split("\\s+")[length - 1];
				else
					return functionStr.trim();
			case 1:
				return functionStr;
			default: // mode = 2, return argstr
				return null;
			}
		}
	}
	
	/**
	 * this method used to check if a method has return value or not if the
	 * method is constructor or the method return type is void then it will
	 * return false, otherwise return true
	 * 
	 * @return
	 */
	public static String getMethodRetTyp(String methodStr) {
		String methodName = getMethodInfos(methodStr.trim());
		// constructors have no return type
		if (methodName.endsWith(".new"))
			return null;

		if (methodName.split("\\s+").length > 1) {
			String[] infos = methodName.split("\\s+");
			return infos[infos.length - 2];
		} else
			return "*";
	}
	
	public static String formatSig(String str) {
		String[] infos = getClassMethArgInfos(str);
		return infos[0]+"."+infos[1]+"("+infos[2]+")";
	}
	
	/**
	 * This function returns proper information about a method
	 * 
	 * @param methodString
	 * @return classInfo[0]: package name; classInfo[1]: method name;
	 *         classInfo[2]: argument string; classInfo[3]: returnType
	 */
	public static String[] getClassMethArgInfos(String methodString) {
		String className = "";
		String methodName = getMethodName1(methodString);
		String methodArgs = getfunArgstr(methodString);
		String returnType = getMethodRetTyp(methodString);

		// the constructor case
		if (methodName.endsWith(".new")) {
			// package class name
			className = methodName.substring(0, methodName.length() - 4);
			returnType = null;
			methodName = "new";
		} else {
			if (methodName.indexOf('.') != -1
					&& methodName.indexOf('.') + 1 < methodName.length()) {
				// package class name
				className = methodName.substring(0,methodName.lastIndexOf('.'));
				// method name, null for constructor case
				methodName = methodName.substring(methodName.lastIndexOf('.') + 1);
			} else {
				className = methodName;
				methodName = "";
			}
		}
		return new String[] { className, methodName, methodArgs, returnType };
	}
	
	public static String getClassName(String methodString) {
		String className = "";
		String methodName = getMethodName1(methodString);
		if (methodName.endsWith(".new")) {
			className = methodName.substring(0, methodName.length() - 4);
		} else {
			if (methodName.indexOf('.') != -1 && methodName.indexOf('.') + 1 < methodName.length()) 
				className = methodName.substring(0,methodName.lastIndexOf('.'));
			else 
				className = methodName;
			
		}
		return className;
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


	public static String getInvokeMethoSig(Method run) {
		StringBuilder methodName = new StringBuilder();
		methodName.append(run.getReturnType().getName() + " ");
		methodName.append(run.getDeclaringClass().getName() + "."
				+ run.getName() + "(");

        Type[] paras = run.getParameterTypes();
        if(paras != null && paras.length > 0) {
            for (int i = 0; i < paras.length; i++) {
                methodName.append(trimClassName(paras[i].toString()));
                if (i != paras.length - 1)
                    methodName.append(",");
            }
        }
		return methodName.append(")").toString();
	}

	public static boolean matchStack4Constr(Stack<String> events, Constructor<?> run) {
		String className = trimClassName(run.getDeclaringClass().toString());
		if (events != null && !events.isEmpty()
				&& events.peek().contains(className))
			return true;

		return false;
	}

	public static boolean matchSig(String matchingVal, Method run) {
		String methodSig = getInvokeMethoSig(run);
		String methodName = getMethodName1(methodSig);
		String argStr = getfunArgstr(methodSig);
		if (argStr == null || argStr.trim().length() == 0)
			argStr = "";
		methodSig = methodName + "(" + argStr + ")";
		return isMatching(methodSig, matchingVal);
	}

	public static String getConstruSig(Constructor<?> run) {
		String className = trimClassName(run.getDeclaringClass().toString())
				+ ".new";

		Type[] paras = run.getGenericParameterTypes();
		if(paras != null && paras.length > 0) {
			StringBuilder argStr = new StringBuilder();

			//Parameter[] paras = run.getParameters();
			for (int i = 0; i < paras.length; i++) {
				String temp = paras[i].toString();
				argStr.append(trimClassName(temp));
				if (i != paras.length - 1)
					argStr.append(",");
			}
			className += "(" + argStr.toString() + ")";
		}
		return className;
	}

	/**
	 * Join a string array with a separator
	 * 
	 * @param strArr
	 * @param separator
	 * @return
	 */
	 
	static String strArrJoin(Set<String> strArr, String separator) {
		StringBuilder sbStr = new StringBuilder();
		for(String str: strArr)
			sbStr.append(str + separator);
		return sbStr.substring(0, sbStr.length()-1);
	}
 
	static boolean matchSig(String matchingVal, Constructor<?> run) {
		String regex = getConstruSig(run);
		return isMatching(regex, matchingVal);
	}

	static boolean isMatching(String reg, String str4Match) {
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(str4Match);
		return matcher.find();
	} 

	static boolean matchingStack(Stack<String> events, Method run) {
		String className = trimClassName(run.getDeclaringClass().toString());
		className = concatClsMethod(className, run.getName());
		if (events != null && !events.isEmpty() && events.peek().contains(className))
			return true;

		return false;
	}
	
	private static String concatClsMethod(String className, String methodName) {
		if (methodName == null || methodName.trim().length() == 0)
			return className.trim();
		else
			return className.trim().concat(".").concat(methodName.trim());
	}
}