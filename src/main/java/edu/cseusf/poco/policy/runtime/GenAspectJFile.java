package edu.cseusf.poco.policy.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenAspectJFile {
	private PrintWriter out;
	private final int indentLevel = 0;
	private static int pointCount = 0;

	public void gen(String pkgname, Set<String> concernedEvts, String outputpath) {

		File writeTo = new File(outputpath +"/PoCoPolicies.aj");
		try {
			out = new PrintWriter(writeTo);
			// step 1: aspectjPrologue
			outAspectJPrologue(pkgname);
			// step 2: generate advices for those methods that need monitor both
			genPointCuts(concernedEvts);
			// step 3: generate advices for constructors
			outAdvice4setOutput();
			// step 4: aspectjEpilogue
			outAspectJEpilogue();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (out != null)
				out.close();
			else
				System.err.println("PrintWriter not open");
		}
	}

	private void outAspectJEpilogue() {
		outLine(0, "}");
	}

	private void outAspectJPrologue(String pkgname) {
		outLine(0, "package " + pkgname + ";\n");
		outLine(0, "import java.lang.reflect.Constructor;\n");

		outLine(0, "import edu.cseusf.poco.event.Action;");
		outLine(0, "import edu.cseusf.poco.event.Result;");
		outLine(0, "import edu.cseusf.poco.event.Event;");
		outLine(0, "import edu.cseusf.poco.event.EvtTyp;");
		outLine(0, "import edu.cseusf.poco.policy.Policy;");
		outLine(0, "import edu.cseusf.poco.policy.Monitor;\n");
		
		outLine(0, "public aspect PoCoPolicies {\n");
		
		outLine(1, "private Monitor pocoPolicy;");
		outLine(1, "private Event trigger;");
		outLine(1, "private Object returnRes;\n");
		
		outLine(1, "public PoCoPolicies() {");
		
		outLine(2, "pocoPolicy = PoCoDemo.getPocoPolicy();");
		// add policies
		outLine(1, "}\n");
	}
 

	private void genPointCuts(Set<String> sigs) {
		Set<String> temp = new HashSet<String>();
		for (String sig : sigs) {
			if (!isAction(sig))
				temp.add(sig);
		}
		sigs.removeAll(temp);
		if (sigs.size() == 0)
			return;

		outLine(1, "pointcut PointCut%s():", pointCount);
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (String sig : sigs) {
			if (count != 0)
				sb.append("\t\t");
			sb.append("call(");
			if (isConstructor(sig)) {
				sig = formatName4Constructor(sig);
			} else {
				sb.append("* ");
			}
			sb.append(sig + ")");
			if (++count < sigs.size())
				sb.append(" || \n");
		}
		outLine(2, "%s;\n", sb.toString());

		outLine(1, "Object around(): PointCut%s() {", pointCount++);
		outLine(2, "if (pocoPolicy.isLocked4Oblig()) {");
		outLine(3, "Object obj = proceed();");
		outLine(3, "pocoPolicy.getRtrace().addRes(new Result(thisJoinPoint, obj));");
		outLine(3, "return obj;");
		outLine(2, "} else {");
		outLine(3, "trigger = new Action(thisJoinPoint); ");
		outLine(3, "returnRes = null;\n");
		outLine(3, "pocoPolicy.processTrigger(new Action(thisJoinPoint));");
		outLine(3, "if (Policy.outputNotSet() || trigger.equals(Policy.getOutput())) {");
		outLine(4, "returnRes = proceed();");
		outLine(3, "} else {");
		outLine(4, "Event e = Policy.getOutput();");
		outLine(4, "if (e == null)	{");
		outLine(5, "System.out.println(\"the output event is empty.\");");
		outLine(5, "System.exit(-1); ");
		outLine(4, "}");
		outLine(4, "returnRes = e.getEventTyp() == EvtTyp.RESULT ? ((Result) e).getEvtRes() : ((Action) e).execute(); ");
		outLine(3, "}");
		outLine(3, "pocoPolicy.processTrigger(new Result(thisJoinPoint, returnRes));");
		outLine(3, "if (Policy.outputNotSet()) {");
		outLine(4, "return returnRes;");
		outLine(3, "} else {");
		outLine(4, "Event e = Policy.getOutput();");
		outLine(4, "if (e == null) returnRes = e;");
		outLine(5, "returnRes = (e.getEventTyp() == EvtTyp.RESULT) ? ((Result) e).getEvtRes() : ((Action) e).execute();");
		outLine(3, "}");
		outLine(3, "return returnRes;");
		outLine(2, "}");
		
		outLine(1, "}\n");
	}

	private void outAdvice4setOutput() {
		outLine(1, "pointcut PointCut%d():", pointCount);
		outLine(2, "call( * edu.cseusf.poco.policy.Policy+.setOutput(edu.cseusf.poco.event.Event) );\n");

		outLine(1, "Object around(): PointCut%s() {", pointCount++);
		outLine(2, "Object obj = proceed();");
		outLine(2, "pocoPolicy.getRtrace().addRes(new Result(\"edu.cseusf.poco.policy.Policy.setOutput(edu.cseusf.poco.event.Event)\", obj));");
		outLine(2, "return obj;");
		outLine(1, "}\n");
	}

	private boolean isConstructor(String sig) {
		Pattern pat = Pattern.compile("^(.+)\\.<init>\\((.*)\\)$");
		Matcher matcher = pat.matcher(sig);
		return matcher.find();
	}

	private String formatName4Constructor(String sig) {
		Pattern pat = Pattern.compile("^(.+)\\.(<init>)\\((.*)\\)$");
		Matcher matcher = pat.matcher(sig);
		return matcher.find() ? sig.replace(matcher.group(2), "new") : sig;
	}

	private boolean isAction(String str) {
		if (str == null)
			return false;
		Pattern pattern = Pattern.compile("^(.+)\\((.*)\\)$");
		Matcher matcher = pattern.matcher(str);
		return matcher.find();
	}

	/**
	 * Outputs one line of Java/AspectJ code to the out object (always ends in
	 * newline).
	 *
	 * @param indent
	 *            indent level of current line (relative to the existing indent
	 *            level)
	 * @param text
	 *            code to write out (printf style formatting used)
	 * @param args
	 *            printf-style arguments
	 */
	private void outLine(int indent, String text, Object... args) {
		outPartial(indent, text, args);
		outPartial(-1, "\n");
	}

	private void outPartial(int indent, String text, Object... args) {
		if (indent >= 0) {
			int trueIndent = (indent + indentLevel) * 4;
			for (int i = 0; i < trueIndent; i++)
				out.format(" ");
		}
		out.format(text, args);
	}
}