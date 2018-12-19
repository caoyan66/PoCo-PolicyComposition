package edu.cseusf.poco.policy.staticAnalysis.visitClasses;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.asm5.ClassVisitor;
import org.kohsuke.asm5.Label;
import org.kohsuke.asm5.MethodVisitor;
import org.kohsuke.asm5.Opcodes;
import org.kohsuke.asm5.tree.LocalVariableNode;
import org.kohsuke.asm5.tree.MethodNode;

import edu.cseusf.poco.policy.Utils;

public class PolicyMethodVisitor extends MethodVisitor {
    List<LocalVariableNode> _localVars;
    private ArrayList<String> _methodList;
    private ClassVisitor _policyVisitor;
    private String _currMtdName;
    private String _desc; 
    private Map<String, ArrayList<String>> _mthName_2_EvtLists;
    
    public PolicyMethodVisitor(ClassVisitor pv, MethodNode mNode, Map<String, ArrayList<String>> mthName_2_EvtLists) {
    	 super(Opcodes.ASM5);
         _localVars = mNode.localVariables;
         _currMtdName = mNode.name;
         _desc = mNode.desc;
         _policyVisitor = pv;
         _mthName_2_EvtLists = mthName_2_EvtLists;
         _methodList = new ArrayList<String>();
	}
	
    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
    	if(!name.equals("this") && isPara(name)) {
    		String type = Utils.formatClassName(desc);
    		if(type.equals("edu.cseusf.event.Event"))
    			_methodList.add("$" + name + "$" + Utils.formatClassName(desc));
    	}
    }
    
    private boolean isPara(String name) {
		for(int i = 0; i<_localVars.size(); i++) {
			if(_localVars.get(i).name.equals(name))
				return true;
		}
		return false;
	}

	@Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    	String sig = Utils.formatClassName(owner) + "." + name + "("+ Utils.formatArgList(desc) +")";
    	_methodList.add(sig);
    }
    
    @Override
    public void visitEnd() {
    	_mthName_2_EvtLists.put(_currMtdName, _desc,  _methodList);
    }
}