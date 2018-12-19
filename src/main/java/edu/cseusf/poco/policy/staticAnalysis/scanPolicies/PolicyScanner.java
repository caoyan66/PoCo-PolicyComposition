package edu.cseusf.poco.policy.staticAnalysis.scanPolicies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
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
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

public class PolicyScanner extends TreeScanner<Void, Void> {
	private boolean _isPoCoPolicy;
	private static ArrayList<String> _declaredPolicies;
	
	private String _className;
	private Map<String, VariableObject> _var2TypVal;
	private Stack<String> _currVar4Assignment;
	private Stack<String> _currInvokeMtd;
 
	private String _currMtdName;
	private LinkedHashMap<String, String> _argsNam2Typ4currMtd;
	
	private ArrayList<EventInfo> _methodSigs;
	private LinkedHashMap<String, ArrayList<EventInfo>> _mthName2Evtsigs;
	private Set<String> _secReleEvts;
	private Map<String, HashSet<String>> _abs2ConcreteAct;
	
	public PolicyScanner(Map<String, HashSet<String>> abs2ConcreteAct) {
		initMtdName2Evtsigs();
		_var2TypVal = new HashMap<String, VariableObject>();
		_methodSigs = new ArrayList<EventInfo>();
		_argsNam2Typ4currMtd = new LinkedHashMap<>();
		_currVar4Assignment = new Stack<String>();
		_currInvokeMtd = new Stack<String>();
		_secReleEvts = new HashSet<String>(); 
		_isPoCoPolicy = false;
		_abs2ConcreteAct = abs2ConcreteAct;
	}
	
	private void initMtdName2Evtsigs() {
		_mthName2Evtsigs = new LinkedHashMap<String, ArrayList<EventInfo>>();
		_mthName2Evtsigs.put("onTrigger", new ArrayList<>());
		_mthName2Evtsigs.put("vote", new ArrayList<>());
		_mthName2Evtsigs.put("onOblig", new ArrayList<>());
	}

	@Override
	public Void visitClass(ClassTree node, Void p) {
		if (node.getExtendsClause() != null) {
			String subClassName = node.getExtendsClause().toString();
			_className = node.getSimpleName().toString();
			if (subClassName.equals("Policy") && _declaredPolicies.contains(_className)) {
				_isPoCoPolicy = true;
				return super.visitClass(node, p);
			}
		}
		return null;
	}

	@Override
	public Void visitMethod(MethodTree node, Void p) {
		_currMtdName = node.getName().toString();
		_argsNam2Typ4currMtd = new LinkedHashMap<>();
		node.getParameters().forEach(arg->{
			_argsNam2Typ4currMtd.put(arg.getName().toString(),arg.getType().toString());});

		if (!_currMtdName.equals("<init>"))  
			addNewMethNode(_currMtdName);	
		return super.visitMethod(node, p);
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
			// variable is not initialized
			if(_methodSigs.size() >0 ){
				EventInfo temp = _methodSigs.get(_methodSigs.size()-1);
				if(ParsFlgConsts.isCatchStatement(temp.getSig())) 
					temp.setProperty(varType); //add exception type to catch flag
			}
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
		case "Action": 
		case "Result":
			String realType = tree.getIdentifier().toString();
			var = new VariableObject(realType, null, true);
			MtdArgs args = handleVarArgs(tree.getArguments(), p);
			var.setVarArgs(args);
			String argStr = genArgStr(args);
			addEvtSig2List(realType + ".<init>(" + argStr + ")", args, true);
			break;
			
		default: 
			MtdArgs mtdArgs = handleVarArgs(tree.getArguments(), p);
			var.setVarArgs(mtdArgs);
			addEvtSig2List(varType + ".<init>(" + genArgStr(mtdArgs) + ")", mtdArgs, true);
			break;
		}
		return var;
	}

	private MtdArgs handleVarArgs(List<? extends ExpressionTree> args, Void p) {
		if (args == null || args.isEmpty()) return null;
		
		handleArguments(args, p);

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
				
			case IDENTIFIER:
				resolveable = handleIdCase4arg(args, resolveable, obj4Arg, argType, isTrigEvt, i);
				break;
				
			case INT_LITERAL:
				obj4Arg[i] = new Integer(args.get(i).toString());
				argType[i] = "int";
				resolveable = true; 
				break;
				
			case NEW_CLASS: 
				isTrigEvt[i] = false;
				NewClassTree tree = (NewClassTree)args.get(i);
				argType[i] = tree.getIdentifier().toString(); 
				MtdArgs arguments = handleVarArgs(tree.getArguments(), p);
				if(isPrimitiveTyp(argType[i])) {
					obj4Arg[i]  = getPrimitiveValue(argType[i], tree.getArguments());
					resolveable = (tree.getArguments() != null);
				}else {
					obj4Arg[i]  = arguments;
					resolveable = (obj4Arg[i] == null) ? true: arguments.isResolvable();
				}
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

	private boolean handleIdCase4arg(List<? extends ExpressionTree> args, boolean resolveable, Object[] obj4Arg,
			String[] argType, boolean[] isTrigEvt, int i) {
		String varName = args.get(i).toString();
		if( _var2TypVal.containsKey(varName) ) {
			if(_var2TypVal.get(varName) != null && _var2TypVal.get(varName).isResolvable()) {
				obj4Arg[i] = _var2TypVal.get(varName).getVarVal();
				argType[i] = _var2TypVal.get(varName).getVartype();
			}else
				resolveable = false;
		} 
		
		if( visitingOnTriggerMtd()  && varName.equals(arg4PoCoMtd()) ) {
			argType[i] = "edu.cseusf.poco.event.Event";
			isTrigEvt[i] = true;
		} 
		else if( visitingVoteMtd() && varName.equals(arg4PoCoMtd()) ) {
			argType[i] = "edu.cseusf.poco.policy.CFG";
		}
		else if( visitingOnObligMtd() && varName.equals(arg4PoCoMtd()) ) {
			argType[i] = "edu.cseusf.poco.policy.Rtrace";
		}
		return resolveable;
	}

	private boolean isVisitingPolicyOVO() {
		return visitingOnTriggerMtd() || visitingVoteMtd() || visitingOnObligMtd();
	}
	private boolean visitingOnTriggerMtd()  { 
		if ( matchingMtd("onTrigger") && _argsNam2Typ4currMtd.size() == 1) {
			String varName = getCurrMtdArgSet()[0];
			String argTyp = _argsNam2Typ4currMtd.get(varName);
			return argTyp.equals("Event") || argTyp.equals("edu.cseusf.poco.event.Event");
		}
		return false;
	}
	private boolean visitingVoteMtd() { 
		return matchingMtd("vote") && matchingArgs(1, new String[] {"CFG"});
	}
	private boolean visitingOnObligMtd()  { 
		return matchingMtd("onOblig") && matchingArgs(1, new String[] {"Rtrace"});
	}
	private boolean matchingMtd(String mtdName) {
		return _currMtdName!= null && _currMtdName.equals(mtdName);
	}
	
	private boolean matchingArgs(int length, String[] varTyps) {
		assert varTyps != null && varTyps.length == length;
		
		if(_argsNam2Typ4currMtd.size() == length) {
			boolean isMatch = true;
			String[] vars = getCurrMtdArgSet();
			
			for(int i = 0; i<vars.length; i++) {
				String varType = _argsNam2Typ4currMtd.get(vars[i]);
				if(!varType.equals(varTyps[i])) {
					isMatch = false;
					break;
				}
			}
			return isMatch;
		}
		return false;
	}
	
	private String argName4CurrMtd(int index) {
		if(_argsNam2Typ4currMtd.size() == 0 || index <0 || index >= _argsNam2Typ4currMtd.size())
			return null;
		String[] vars = getCurrMtdArgSet();
		return vars[index];
	}
	
	private String[] getCurrMtdArgSet() {
		assert _argsNam2Typ4currMtd != null;
		Set<String> keys = _argsNam2Typ4currMtd.keySet();
		return  keys.toArray(new String[keys.size()]);
	}
	
	private String arg4PoCoMtd() {
		Set<String> keys= _argsNam2Typ4currMtd.keySet();
		String[] vars = keys.toArray(new String[keys.size()]);
		return vars[0];
	}
	
	@Override
	public Void visitExpressionStatement(ExpressionStatementTree node, Void p) {
		handleExpression(node.getExpression(), p, false);
		return null;
	}

	private void handleExpression(ExpressionTree etree, Void p, boolean handlingMtdArg) {
		if (etree == null) 	return;
		
		switch (etree.getKind()) {
		case GREATER_THAN:  
		case GREATER_THAN_EQUAL: 
		case LESS_THAN: 	
		case LESS_THAN_EQUAL: 
		case EQUAL_TO: 		
		case NOT_EQUAL_TO:		 
		case CONDITIONAL_AND: 	
		case CONDITIONAL_OR:	handleAndOrCase4Expr(etree, p);						break;
		
		case ASSIGNMENT: 		handleAssigmentCase4Expr(etree, p); 				break;
		
		case METHOD_INVOCATION: handleMtdInvokeCase4Expr(etree, p);					break;
		
		case NEW_CLASS:			handleNewCase4Expr(etree, p, handlingMtdArg);		break;
		
		case IDENTIFIER:		handleIdcase4Expr(etree, handlingMtdArg);			break;
		
		case PLUS:				handlePluscase4Expr(etree, p);						break;
		
		case PARENTHESIZED:     handleExpr4IfCondition(((ParenthesizedTree) etree).getExpression(), p);	 break;
		
		case LOGICAL_COMPLEMENT:handleExpression(((UnaryTree) etree).getExpression(), p, handlingMtdArg);break;
		
		case CONDITIONAL_EXPRESSION:handleConditional4Expr(etree, p);									 break;
		default: break;
		}
	}

	private void handleConditional4Expr(ExpressionTree etree, Void p) {
		ConditionalExpressionTree ceTree = (ConditionalExpressionTree) etree;
		_methodSigs.add(new EventInfo(ParsFlgConsts.If_CONDITION));
		handleExpression(ceTree.getCondition(), p, false);

		// step 2. handle True Expression
		_methodSigs.add(new EventInfo(ParsFlgConsts.THEN_BRANCH));
		handleExpression(ceTree.getTrueExpression(), p, false);

		// step 3. handle False Expression
		_methodSigs.add(new EventInfo(ParsFlgConsts.ELSE_BRANCH));
		handleExpression(ceTree.getFalseExpression(), p, false);

		// step 4. mark the end to this if statement
		_methodSigs.add(new EventInfo(ParsFlgConsts.ENDOFIF));
	}

	private void handleIdcase4Expr(ExpressionTree etree, boolean handlingMtdArg) {
		String varname = etree.toString();
		boolean isVisitingMatchesAct = visitingMatchesAct();
		boolean isVisitingMatchesRes = visitingMatchesRes();
		
		if(!isVisitingMatchesAct && !isVisitingMatchesRes) 	return;
		
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

	private void handleNewCase4Expr(ExpressionTree etree, Void p, boolean handlingMtdArg) {
		NewClassTree nTree = (NewClassTree) etree;
		List<? extends ExpressionTree> args4Init = nTree.getArguments();
		MtdArgs args = handleVarArgs(args4Init, p);
		
		String newStatementSig = nTree.getIdentifier().toString();
		newStatementSig += ".<init>(" + handleArugmentSig(args4Init) + ")";
		
		addEvtSig2List(newStatementSig, args, checkResolvable(args4Init));
	}
	
	private void handleMtdInvokeCase4Expr(ExpressionTree etree, Void p) {
		MethodInvocationTree mTree = (MethodInvocationTree) etree;
		// step 1. get the invoked method's name and push it onto the stack
		String mtdStr = mTree.getMethodSelect().toString();
		String mtdName = genValidMtdName(mtdStr);
		_currInvokeMtd.push(mtdName);
		
		MtdArgs caller = null;
		if(mtdStr.indexOf('.') != -1) {
			String varName = mtdStr.substring(0,mtdStr.lastIndexOf('.'));
			if( visitingOnTriggerMtd()  && varName.equals(arg4PoCoMtd()) ) {
				boolean[] isTrig = {true};
				caller = new MtdArgs(new Object[]{null},new String[]{"edu.cseusf.poco.event.Event"}, isTrig);
			} 
		}
		
		// step 2. handle arguments for METHOD_INVOCATION (arguments can still be methodInvocation)
		List<? extends ExpressionTree> args = mTree.getArguments();
		MtdArgs mtdArgs = handleVarArgs(args, p);
		
		// step 3. generate valid method name and add it to the list
		boolean isEvtResolvable = checkResolvable(args);
		if(mtdName.startsWith("this.")) mtdName = mtdName.substring(5);
		String mtdSig = mtdName + "(" + handleArugmentSig(args) + ")";
		if(isVisitingPolicyOVO() || (_currMtdName != null && !_currMtdName.equals("<init>"))) 
			_methodSigs.add(new EventInfo(mtdSig, mtdArgs, isEvtResolvable, caller));
		
		// step 4: pop the current invoking method name
		_currInvokeMtd.pop();
	}
	
	private void handlePluscase4Expr(ExpressionTree etree, Void p) {
		BinaryTree bt = (BinaryTree) etree;
		handleExpression(bt.getLeftOperand() , p, false);
		handleExpression(bt.getRightOperand(), p, false);
	}
	
	private void handleAndOrCase4Expr(ExpressionTree etree, Void p) {
		BinaryTree bTree1 = (BinaryTree) etree;
		handleExpr4IfCondition(bTree1.getLeftOperand(), p);
		handleExpr4IfCondition(bTree1.getRightOperand(), p);
	}

	private void handleAssigmentCase4Expr(ExpressionTree etree, Void p) {
		AssignmentTree aTree = (AssignmentTree) etree;
		String varName = aTree.getVariable().toString();
		// step 1. handle the left side of the assignment
		_currVar4Assignment.push(varName);
		// step 2. handle the right hand side of the assignment
		etree = aTree.getExpression();
		handleExpression(aTree.getExpression(), p, false);

		// pop the flag
		_currVar4Assignment.pop();
	}

	private void addEvtSig2List(String mtdSig, MtdArgs args, boolean isEvtResolvable) {
		if(isVisitingPolicyOVO() || (_currMtdName != null && !_currMtdName.equals("<init>"))) 
			_methodSigs.add(new EventInfo(mtdSig, args, isEvtResolvable, null)); 
	}
	  
	private boolean checkResolvable(List<? extends ExpressionTree> args) {
		if (args == null)
			return true;
		for (int i = 0; i < args.size(); i++) {
			Tree arg = args.get(i);
			switch (arg.getKind()) {
			case IDENTIFIER:
				String argName = arg.toString();
				if (_var2TypVal.containsKey(argName)) {
					VariableObject argObj = _var2TypVal.get(argName);
					if (!argObj.isResolvable()) return false;
				}
				else  return false;
				break; 
			default: break;
			}
		}
		return true;
	}

	private String handleArugmentSig(List<? extends ExpressionTree> args) {
		if (args == null) return "";

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.size(); i++) {
			Tree arg = args.get(i);
			switch (arg.getKind()) {
			case NULL_LITERAL:
			case METHOD_INVOCATION:	
				sb.append("NULL");		break;
			case BOOLEAN_LITERAL:	
				sb.append("boolean");	break;
			case INT_LITERAL: 	 	
				sb.append("int");		break;
			case PLUS:				
				sb.append("NULL");		break;
			case STRING_LITERAL: 	
				sb.append("java.lang.String");	break;
			case NEW_CLASS:			
				NewClassTree newClassTree = (NewClassTree) arg;
				sb.append( newClassTree.getIdentifier().toString() ); break;
			case IDENTIFIER: 		
				sb.append( handleId(arg.toString()) ); break;
			case NEW_ARRAY:			
				sb.append( ((NewArrayTree)arg).getType()+"[]" );break;
			default: break;
			}
			if (i != args.size() - 1) sb.append(",");
		}
		return sb.toString();
	}

	private String handleId(String argName) {
		if (_var2TypVal.containsKey(argName)) {
			VariableObject argObj = _var2TypVal.get(argName);
			return argObj.getVartype();
		} else  
			return checkMtdArgs(argName);
	}

	private String checkMtdArgs(String argName) {
		if(_argsNam2Typ4currMtd != null && !_argsNam2Typ4currMtd.isEmpty()) {
			Set<String> vars = _argsNam2Typ4currMtd.keySet();
			for(String var: vars) {
				if(var.equals(argName))
					return _argsNam2Typ4currMtd.get(var);
			}
		}
		return null;
	}

	private String genValidMtdName(String mthSig) {
		String mthObj = mthSig;
		if (mthSig.indexOf('.') != -1)
			mthObj = mthSig.substring(0, mthSig.indexOf('.'));
		
		if(visitingOnTriggerMtd() && mthObj.equals(argName4CurrMtd(0)))
			mthSig = "edu.cseusf.poco.event.Event" + mthSig.substring(mthSig.indexOf('.'));
		else if (visitingVoteMtd() && mthObj.equals(argName4CurrMtd(0)))
				mthSig = "edu.cseusf.poco.policy.Rtrace" + mthSig.substring(mthSig.indexOf('.'));
		else if (visitingOnObligMtd() && mthObj.equals(argName4CurrMtd(0)))
			mthSig = "edu.cseusf.poco.policy.CFG" + mthSig.substring(mthSig.indexOf('.'));
		else {
			if (_var2TypVal.containsKey(mthObj))
				mthSig = _var2TypVal.get(mthObj).getVartype() + mthSig.substring(mthSig.indexOf('.'));
		}
		return mthSig;
	}
	
	private void addNewMethNode(String mtdName) {
		_methodSigs = new ArrayList<>();
		_mthName2Evtsigs.put(mtdName, _methodSigs);
	}
	
	private void handleStatement(StatementTree tree, Void p) {
		switch (tree.getKind()) {
		case IF: 					
			visitIf((IfTree) tree, p); 										
			break;
		case TRY: 					
			visitTry((TryTree) tree, p); 									
			break;
		case BLOCK: 				
			handleBlock((BlockTree) tree, p); 								
			break;
		case RETURN: 				
			visitReturn((ReturnTree) tree, p);								
			break;
		case SWITCH: 				
			handleExpression(((SwitchTree) tree).getExpression(), p,false);	
			break;
		case EXPRESSION_STATEMENT: 	
			visitExpressionStatement((ExpressionStatementTree) tree, p);	
			break;
		case VARIABLE:   			
			visitVariable( (VariableTree) tree, p);							
			break;	
		default: break;
		}
	}
	
	private void handleArguments(List<? extends ExpressionTree> args, Void p) {
		if(args == null || args.size() == 0) 
			return;
		args.forEach( arg -> handleExpression(arg, p, true));
	}

	private boolean visitingMatchesAct() { return  visitingMatchesMtd("edu.cseusf.poco.event.Event.matchesAct"); }
	private boolean visitingMatchesRes() { return  visitingMatchesMtd("edu.cseusf.poco.event.Event.matchesRes"); }
	private boolean visitingMatchesMtd(String mtdSig) {
		if(isVisitingPolicyOVO() && !_currInvokeMtd.isEmpty()) 
			return _currInvokeMtd.peek().equals(mtdSig);
		return false;
	}
	
	private void handleExpr4IfCondition(ExpressionTree condition, Void p) {
		if (condition == null) 
			return;
		handleExpression(condition, p, false);
	}

	@Override
	public Void visitIf(IfTree node, Void p) {
		// step 1: handle condition
		_methodSigs.add(new EventInfo(ParsFlgConsts.If_CONDITION));
		handleExpression(node.getCondition(), p, false);
		
		// step 2. handle then statement
		_methodSigs.add(new EventInfo(ParsFlgConsts.THEN_BRANCH));
		handleStatement(node.getThenStatement(), p);
		
		// step 3. handle else statement
		if (node.getElseStatement() != null) {
			_methodSigs.add(new EventInfo(ParsFlgConsts.ELSE_BRANCH));
			handleStatement(node.getElseStatement(), p);
		}
		
		// step 4. mark the end to this if statement
		_methodSigs.add(new EventInfo(ParsFlgConsts.ENDOFIF));
		return null;
	}

	@Override
	public Void visitTry(TryTree node, Void p) {
		if (node == null)  return null;
		
		_methodSigs.add(new EventInfo(ParsFlgConsts.TRY));
		List<? extends Tree> trees = node.getResources();
		if(trees.size() >0) {
			_methodSigs.add(new EventInfo(ParsFlgConsts.TRY_RESOURCE));
			trees.forEach( tree -> visitVariable((VariableTree) tree, p) );
		}

		// step 1. handle try block
		_methodSigs.add(new EventInfo(ParsFlgConsts.TRY_BLOCK));
		handleBlock(node.getBlock(), p);
		
		// step 2. handle catch blocks
		node.getCatches().forEach(catchTree -> {if (catchTree != null) {
				_methodSigs.add(new EventInfo(ParsFlgConsts.CATCH_BLOCK));
				visitVariable((VariableTree) catchTree.getParameter(), p);
				handleBlock(catchTree.getBlock(), p);
			}});
		
		// step 3. handle final blocks
		if(node.getFinallyBlock() != null) {
			_methodSigs.add(new EventInfo(ParsFlgConsts.TRY_FINAL));
			handleBlock(node.getFinallyBlock(), p);
		}
		_methodSigs.add(new EventInfo(ParsFlgConsts.ENDTRY));
		return null;
	}

	private void handleBlock(BlockTree bt, Void p) {
		if (bt == null) return;
		bt.getStatements().forEach(statement -> handleStatement(statement, p));
	}

	@Override
	public Void visitReturn(ReturnTree node, Void p) {
		if (node.getExpression() != null) 
			handleExpression(node.getExpression(), p, false);
		return null;
	}
	 
	private String genArgStr(MtdArgs args) {
		if (args == null || args.getArgs() == null)		return "";
		
		String[] argTyps =args.getArgTyps();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < argTyps.length; i++) 
			sb.append(argTyps[i]+ ",");
		return sb.substring(0, sb.length() - 1);
	}
	
	private boolean isPrimitiveTyp(String type) {
		switch (type) {
		case "Boolean":	
		case "Byte":	
		case "Character":
		case "Integer": 
		case "Short": 	
		case "Long":
		case "Double":	
		case "Float":	
			return true;
		default: 
			return false;
		}
	}
	
	private Object getPrimitiveValue(String type, List<? extends ExpressionTree> arglist) {
		if(arglist == null || arglist.size() == 0) return null;
		ExpressionTree arg0 = arglist.get(0);
		if(arg0 == null) return null;
		switch (type) {
		case "Boolean":		
			return new Boolean(arg0.toString());
		case "Byte":		
			return new Byte(arg0.toString());
		case "Character":	
			return new Character(arg0.toString().charAt(0));
		case "Integer": 	
			return new Integer(arg0.toString());
		case "Short": 		
			return new Short(arg0.toString());
		case "Long":		
			return new Long(arg0.toString());
		case "Double":		
			return new Double(arg0.toString());
		case "Float":		
			return new Float(arg0.toString());
		default: 
			return null;
		}
	}
	
	// getters and setters
	public Map<String, VariableObject> get_closure() { return _var2TypVal; }
	public String get_className() { return _className; }
	public Set<String> getSecReleEvts() { return _secReleEvts; }
	public ArrayList<EventInfo> getMethodSigs() { return _methodSigs; }
	public Map<String, ArrayList<EventInfo>> getMthName2Evtsigs() { return _mthName2Evtsigs; }
	public boolean isPoCoPolicy() { return _isPoCoPolicy; }
	public Map<String, HashSet<String>> getAbs2ConcreteAct() { return _abs2ConcreteAct;}
	public static ArrayList<String> getDeclaredPolicies() { return _declaredPolicies;}
	public static void setDeclaredPolicies(ArrayList<String> _policyNames) { _declaredPolicies = _policyNames;}
}