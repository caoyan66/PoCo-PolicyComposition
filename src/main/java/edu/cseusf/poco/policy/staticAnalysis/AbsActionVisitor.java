package edu.cseusf.poco.policy.staticAnalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;

public class AbsActionVisitor extends TreeScanner<Void, Void> {
	private String className = null;
	private Map<String, HashSet<String>> absName2Sigs;

	public AbsActionVisitor() {
		this.absName2Sigs = new HashMap<String, HashSet<String>>();
	}

	@Override
	public Void visitClass(ClassTree node, Void p) {
		String subClassName = null;
		if (node.getExtendsClause() != null) {
			subClassName = node.getExtendsClause().toString();
			if (subClassName.equals("AbsAction")) {
				className = node.getSimpleName().toString();
				return super.visitClass(node, p);
			}
		}
		return null;
	}

	@Override
	public Void visitMethod(MethodTree node, Void p) {
		return node.getName().toString().equals("mapConc2Abs") ? super.visitMethod(node, p) : null;
	}

	@Override
	public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
		String sig = node.getMethodSelect().toString();
		if (sig.endsWith(".matches")) {
			if (node.getArguments() != null) {
				
				String mthStr = getEvtSig(node.getArguments().get(0).toString(),"Action");
				// successfully located the method signature
				if (mthStr != null && isMethod(mthStr)) {
					if(absName2Sigs.containsKey(className)) {
						absName2Sigs.get(className).add(mthStr);
					}else {
						HashSet<String> newSet = new HashSet<String>();
						newSet.add(mthStr);
						absName2Sigs.put(className, newSet);
					}
				}
			}
		}
		return null;
	}

	private String getEvtSig(String str, String type) {
		if (str == null) return null;
		
		Pattern pattern = Pattern.compile("^\\s*new\\s+"+type+"\\s*\\(\\s*\"(.+)\"\\s*\\)\\s*$");
		Matcher matcher = pattern.matcher(str);
		return matcher.find() ? matcher.group(1).trim() : null;
	}

	private boolean isMethod(String mtdStr) {
		if (mtdStr == null) return false;
		
		Pattern pattern = Pattern.compile("^(.+)\\((.*)\\)$");
		Matcher matcher = pattern.matcher(mtdStr);
		return matcher.find();
	}
	
	//getter and setter
	public Map<String, HashSet<String>> getAbsName2Sigs() { return absName2Sigs; }
}