package edu.cseusf.poco.policy.staticAnalysis.visitClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.asm5.ClassVisitor;
import org.kohsuke.asm5.MethodVisitor;
import org.kohsuke.asm5.tree.ClassNode;
import org.kohsuke.asm5.tree.MethodNode;

import edu.cseusf.poco.policy.Utils;
import edu.cseusf.poco.policy.staticAnalysis.scanPolicies.VariableObject;

public class PolicyVisitor extends ClassVisitor{
	private final int api; 
	private String _visitingClzName;
	private List<MethodNode> _methodNodes;
	private Map<String, VariableObject> _closure;
	private Map<String, ArrayList<String>> _mthName_2_EvtLists;
		
	public PolicyVisitor(Integer api, ClassVisitor cv, ClassNode cn) {
        super(api, cv);
        this.api =  api;
        _methodNodes=cn.methods;
        _closure = new HashMap<String, VariableObject>();
        _mthName_2_EvtLists = new  HashMap<String, ArrayList<String>>();
    }
	
	@Override
	 public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		 if(superName.equals("edu/cseusf/poco/policy/Policy")) {
			 _visitingClzName = Utils.formatClassName(name);
			 super.visit(version, access, name, signature, superName, interfaces);
		 }
	 } 
 
	 @Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		for(MethodNode mNode: _methodNodes) {
			if(!name.equals("<init>") && mNode.name.equals(name) && mNode.desc.equals(desc))  
				return new PolicyMethodVisitor(this, mNode, _mthName_2_EvtLists); 
		}
		return null;
	}  
	
	//getters and setters
	public int getApi() { return api;}
	public String getClzName () { return _visitingClzName; }
	public Map<String, VariableObject> get_closure() { return _closure; }
	public Map<String, ArrayList<String>> getMthName2Evtsigs() { return _mthName_2_EvtLists; }
}