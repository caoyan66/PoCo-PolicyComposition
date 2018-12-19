package edu.cseusf.poco.event;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import edu.cseusf.poco.policy.runtime.RuntimeUtils;

public class Promoter {
	public static Object Reflect(Action act) {
		try {
			String[] info = RuntimeUtils.getClassMethArgInfos(act.getEvtSig());
			String mtdName = info[1];
			Object obj = act.getCaller();
			Class<?> cls1 =  (obj == null) ? Class.forName(info[0]) : obj.getClass();
			return ReflectExecute(obj, cls1, mtdName, act.getArgs());
		}
		catch (Exception e) { failToExectute(act.getEvtSig()); }
		return null;
	}

	private static Object ReflectExecute(Object obj, Class<?> cls1, String mtdName, Object[] args) {
		try {
			return mtdName.equals("<init>") ? execConstructor(cls1, args)
										    : execMethod(obj, cls1, mtdName, args);
			
		} catch (Exception e) { failToExectute(mtdName); }
		
		return null;
	}

	private static Object execMethod(Object obj, Class<?> cls1, String mtdName, Object[] args)  {
		try {
			Method[] methods = cls1.getMethods();
			Method theMtd = null;
			int paramCounts = (args == null) ? 0 : args.length;

			boolean isfound = false;
			for (Method mtd : methods) {
				// if find the method name
				if (mtd.getName().equals(mtdName)) {
					Type[] mtdParams = mtd.getGenericParameterTypes();
					if (mtdParams.length != paramCounts) continue;
					isfound = checkParams(args, paramCounts, mtd.getGenericParameterTypes());
				}	
				if (isfound) { theMtd = mtd; break; }
			}
		
			// found the right method that is we wanted
			if (isfound) {
				System.out.println(theMtd.getDeclaringClass().getClassLoader());
				if (obj != null)
					return theMtd.invoke(obj, args);
				else // need check static or not
					return Modifier.isStatic(theMtd.getModifiers()) ? theMtd.invoke(null, args)
							                                        : theMtd.invoke(cls1.newInstance(), args);
			} else { failToExectute(mtdName); }
		
		}catch(Exception e) { failToExectute(mtdName); }

		return null;
	}

	private static Object execConstructor(Class<?> cls1, Object[] args) {
		try {
			Constructor<?>[] construs = cls1.getConstructors();
			Constructor<?> theConstructor = null;
			int paramCounts = (args == null) ? 0 : args.length;
			
			boolean isfound = false;
			for (Constructor<?> con : construs) {
				if (con.getParameterTypes().length != paramCounts) continue;
				isfound = checkParams(args, paramCounts, con.getGenericParameterTypes());
				if (isfound) { theConstructor = con; break; }
			}
			
			if(isfound)
				return theConstructor.newInstance(args);
			else 
				failToExectute(cls1.getName() +".<init>");
		} catch (Exception e) { failToExectute(cls1.getName() +".<init>"); }
		
		return null;
	}

	private static boolean checkParams(Object[] args, int paramCounts, Type[] conParams) {
		boolean isfound = true;
		if (paramCounts != 0) {
			for(int i = 0; i < paramCounts; i++) {
				if(args[i] == null) continue;
				String conParam = GetTypeName(conParams[i].toString());
				if(conParam.equals("java.lang.Object")) continue;
				String argTyp = args[i].getClass().getName();
				if ( !conParam.equals(argTyp) && !conParam.equals(toPrimitiveType(argTyp)) ) {
					isfound = false; break;
				}
			}
		}
		return isfound;
	}

	private static String GetTypeName(String typeName) {
		// if the type is not the primitive type
		if (typeName.length() > 6 && typeName.substring(0, 5).equals("class"))
			return typeName.substring(6, typeName.length());
		else
			return typeName;
	}
	
	 private static String toPrimitiveType(String type) {
		 switch (type) {
	     	case "java.lang.String":  					return "String";
	        case "Byte": 	case "java.lang.Byte": 		return "byte";
	        case "Integer": case "java.lang.Integer": 	return "int";
	        case "Short":  	case "java.lang.Short":		return "short";
	        case "Long": 	case "java.lang.Long": 		return "long";
	        case "Double":  case "java.lang.Double": 	return "double";
	        case "Float": 	case "java.lang.Float":		return "float";
	        case "Boolean": case "java.lang.Boolean": 	return "boolean";
	        case "Character":case "java.lang.Character":return "char";
			default:   									return type;
	    }
	}

	private static void failToExectute(String sig) {
		System.out.println("Sorry, the event " + sig + " cannot be executed!");
		System.exit(-1);
	}
}