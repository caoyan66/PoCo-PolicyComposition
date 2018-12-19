package edu.cseusf.poco.policy.staticAnalysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

import edu.cseusf.poco.policy.staticAnalysis.scanPolicies.MtdArgs;
import edu.cseusf.poco.policy.staticAnalysis.scanPolicies.VariableObject;

public class PolicyVisitor extends TreeScanner<Void, Void> {
	private Set<String> _secReleEvts; 
	private Map<String, HashSet<String>> _abs2ConcreteAct;
	private Map<String, VariableObject> _var2TypVal;
	private Stack<String> _currVar4Assignment;

	public PolicyVisitor(Map<String, HashSet<String>> abs2ConcreteAct) {
		_abs2ConcreteAct = abs2ConcreteAct;
		_secReleEvts = new HashSet<String>(); 
		_var2TypVal = new HashMap<String, VariableObject>(); 
		_currVar4Assignment = new Stack<String>();
	}
	 
	public Void visitClass(ClassTree node, Void p) {
		if (node.getExtendsClause() != null) {
			String subClassName = node.getExtendsClause().toString();
			if (subClassName.equals("Policy"))
				return super.visitClass(node, p);
		}
		return null;
	}

	public Void visitMethod(MethodTree node, Void p) {
		if(node.getName().toString().equals("onTrigger")) {
			if(node.getParameters().size() == 1) {
				VariableTree s= node.getParameters().get(0);
				String argTyp = s.getType().toString();
				if(argTyp.equals("Event") || argTyp.equals("edu.cseusf.poco.event.Event")) {
					return super.visitMethod(node, p);
				}
			}
		}
		return null;
	}
	
	public Void visitIf(IfTree node, Void p) {
		handleExpression(node.getCondition(), p, false);
		handleStatement(node.getThenStatement(), p);
		if (node.getElseStatement() != null) 
			handleStatement(node.getElseStatement(), p);
		return null;
	}

	private void handleStatement(StatementTree tree, Void p) {
		switch (tree.getKind()) {
		case IF: 					
			visitIf((IfTree) tree, p); 										break;
		case EXPRESSION_STATEMENT: 	
			visitExpressionStatement((ExpressionStatementTree) tree, p);	break;
		case VARIABLE:   			
			visitVariable( (VariableTree) tree, p);							break;	
		default: break;
		}
	}
	 
	public Void visitExpressionStatement(ExpressionStatementTree node, Void p) {
		handleExpression(node.getExpression(), p, false);
		return null;
	}
	
	private void handleExpression(ExpressionTree etree, Void p, boolean handlingMtdArg) {
		if (etree == null) 	return;

		switch (etree.getKind()) {
		case CONDITIONAL_AND: 	
		case CONDITIONAL_OR: 		
			handleAndOrCase4Expr(etree, p);		
			break;	
		case ASSIGNMENT: 			
			handleAssigmentCase4Expr(etree, p); 
			break;
		case METHOD_INVOCATION: 	
			handleMtdInvokeCase4Expr(etree, p);	
			break;
		case IDENTIFIER:			
			handleIdcase4Expr(etree);			
			break;
		case CONDITIONAL_EXPRESSION:
			handleConditional4Expr(etree, p);	
			break;
		case PARENTHESIZED:     	
			handleExpr4IfCondition(((ParenthesizedTree) etree).getExpression(), p);	  	
			break;
		case LOGICAL_COMPLEMENT:	
			handleExpression(((UnaryTree) etree).getExpression(), p, handlingMtdArg);	
			break;
		default: break;
		}
	}
	
	private void handleConditional4Expr(ExpressionTree etree, Void p) {
		ConditionalExpressionTree ceTree = (ConditionalExpressionTree) etree;
		handleExpression(ceTree.getCondition(), p, false);
		handleExpression(ceTree.getTrueExpression(), p, false);
		handleExpression(ceTree.getFalseExpression(), p, false);
	}
	
	private void handleAndOrCase4Expr(ExpressionTree etree, Void p) {
		BinaryTree bTree1 = (BinaryTree) etree;
		handleExpr4IfCondition(bTree1.getLeftOperand(), p);
		handleExpr4IfCondition(bTree1.getRightOperand(), p);
	}
	
	private void handleExpr4IfCondition(ExpressionTree condition, Void p) {
		if (condition == null) return;
		handleExpression(condition, p, false);
	}
	
	private void handleMtdInvokeCase4Expr(ExpressionTree etree, Void p) {
		MethodInvocationTree mTree = (MethodInvocationTree) etree;
		if(isMatchesMtd(mTree)) {
			ExpressionTree et = mTree.getArguments().get(0);
			switch (et.getKind()) {
				case IDENTIFIER:
					handleIdcase4Expr(et);
					break;
				case STRING_LITERAL:
					_secReleEvts.add(et.toString()); 
					break;
				default: break;
			}
		}
	}
	
	private boolean isMatchesMtd(MethodInvocationTree mTree) {
		String mtdName = mTree.getMethodSelect().toString();
		if(mtdName.indexOf('.') != -1) {
			int index = mtdName.lastIndexOf('.');
			String objPart = mtdName.substring(0, index);
			String mtdpart = mtdName.substring(mtdName.lastIndexOf('.')+1);
			
			return nameMatches(mtdpart) && objMatches(objPart);
		}
		return false;
	}
	private boolean nameMatches(String mtdName) {
		return mtdName.equals("matches");
	}
	private boolean objMatches(String objPart) {
		return _var2TypVal.containsKey(objPart) && _var2TypVal.get(objPart).getVartype().equals("Event");
	}
	
	private void handleAssigmentCase4Expr(ExpressionTree etree, Void p) {
		AssignmentTree aTree = (AssignmentTree) etree;
		String varName = aTree.getVariable().toString();
		_currVar4Assignment.push(varName);
		etree = aTree.getExpression();
		handleExpression(aTree.getExpression(), p, false);
		_currVar4Assignment.pop();
	}
	
	private void handleIdcase4Expr(ExpressionTree etree) {
		String varname = etree.toString();
		if( _var2TypVal.containsKey(varname)) {
			String evtType = _var2TypVal.get(varname).getVartype();
			switch (evtType) {
				case "Action":  
				case "Result":
					MtdArgs args = _var2TypVal.get(varname).getVarArgs();
					if(args != null && args.getArgs()!=null)   
						_secReleEvts.add(_var2TypVal.get(varname).getVarArgs().getArgs()[0].toString()); 
					break;
				default:
					if( _abs2ConcreteAct!= null && _abs2ConcreteAct.containsKey(evtType) ) 
						_abs2ConcreteAct.get(evtType).forEach(item->_secReleEvts.add(item));  
					break;
			}	
		}
	}
	
	@Override
	public Void visitVariable(VariableTree node, Void p) {
		// variable with no type declaration and assigned value
		if (node.getType() == null && node.getInitializer() == null) return null;
		
		String varName = node.getName().toString();
		String varType = node.getType().toString();
		ExpressionTree init = node.getInitializer();
		if (init != null) {
			VariableObject var = null;
			switch (init.getKind()) {
				case STRING_LITERAL:
					String val = init.toString();
					if(val.length() > 1 && val.startsWith("\"") && val.endsWith("\""))
						val = val.substring(1, val.length()-1);
					var = new VariableObject("java.lang.String",val , true);
					break;
				case NEW_CLASS: var = handleNewClass4Var(p, varType, init); break;
				case METHOD_INVOCATION:
					handleExpression(node.getInitializer(), p, false);
					var = new VariableObject(varType, null, false);
					break;
				case NULL_LITERAL: var = new VariableObject(varType, null, true); break;
				case IDENTIFIER: 
					String argName = init.toString();
					if (_var2TypVal.containsKey(argName))  var = _var2TypVal.get(argName);
					break;
				case NEW_ARRAY:
					List<? extends ExpressionTree> et = ( (NewArrayTree) init ).getInitializers();
					var = new VariableObject(varType, et.toString(), true);
					break;
				case PLUS:
					var = new VariableObject(varType, null, false);
					break;
				default:
					var = new VariableObject(varType, null, false);
					break;
			}
			_var2TypVal.put(varName, var);
			
		} else {
			VariableObject vObject = new VariableObject(varType, null, true);
			_var2TypVal.put(varName, vObject);
		}
		return super.visitVariable(node, p);
	}
	
	
	private VariableObject handleNewClass4Var(Void p, String varType, ExpressionTree init) {
		VariableObject var;
		NewClassTree tree = (NewClassTree)init;
		var = new VariableObject(varType, null, true);
		switch (varType) {
			case "Action": case "Result":
				String realType = tree.getIdentifier().toString();
				var = new VariableObject(realType, null, true);
				MtdArgs args = handleVarArgs(tree.getArguments(), p);
				var.setVarArgs(args);
				break;
			default:  break;
		}
		return var;
	}
	
	private MtdArgs handleVarArgs(List<? extends ExpressionTree> args, Void p) {
		if (args == null || args.isEmpty()) return null;
		
		boolean resolveable = true;
		Object[] obj4Arg   = new Object[args.size()];
		String[] argType = new String[args.size()];
		boolean[] isTrigEvt = new boolean[args.size()]; Arrays.fill(isTrigEvt, false);
		
		for (int i = 0; i < args.size(); i++) {
			switch (args.get(i).getKind()) {
			case STRING_LITERAL:
				String val = args.get(i).toString();
				obj4Arg[i] = val.substring(1, val.length()-1);
				argType[i] = "java.lang.String";
				break;
				
			case INT_LITERAL:
				obj4Arg[i] = new Integer(args.get(i).toString());
				argType[i] = "int";
				resolveable = true; 
				break;
				
			case NEW_ARRAY:
				NewArrayTree naTree = (NewArrayTree)args.get(i);
				argType[i] = naTree.getType().toString()+"[]";
				MtdArgs initializers = handleVarArgs(naTree.getInitializers(),p);
				obj4Arg[i] = initializers;
				resolveable = initializers.isResolvable(); 
				break;
				
			case BOOLEAN_LITERAL:
				obj4Arg[i] = new Boolean(args.get(i).toString());
				argType[i] = "boolean";  
				resolveable = true; 
				break;
			case METHOD_INVOCATION: argType[i] = "UNKNOWN"; 	resolveable = false; break;
			case NULL_LITERAL: 		argType[i] = "NULL_LITERAL";resolveable = true; break;
			
			default: break;
			}
		}
		return new MtdArgs(obj4Arg, argType, isTrigEvt, resolveable);
	}
	
	public Set<String> getsecReleEvts() { return _secReleEvts; }
}