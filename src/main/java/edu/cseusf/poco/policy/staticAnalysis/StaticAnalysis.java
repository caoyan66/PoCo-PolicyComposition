package edu.cseusf.poco.policy.staticAnalysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.asm5.ClassReader;
import org.kohsuke.asm5.ClassWriter;
import org.kohsuke.asm5.Opcodes;
import org.kohsuke.asm5.tree.ClassNode;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;

import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.GenPolicyCFG;
import edu.cseusf.poco.policy.Policy;
import edu.cseusf.poco.policy.Utils;
import edu.cseusf.poco.policy.runtime.RuntimeUtils;
import edu.cseusf.poco.policy.staticAnalysis.scanPolicies.EventInfo;
import edu.cseusf.poco.policy.staticAnalysis.scanPolicies.ParsFlgConsts;
import edu.cseusf.poco.policy.staticAnalysis.scanPolicies.PolicyScanner;
import edu.cseusf.poco.policy.staticAnalysis.visitClasses.PolicyVisitor;

public class StaticAnalysis {
	private final static int _api = Opcodes.ASM5;
	
	private ArrayList<String> _policyNames;
	private Map<String, Map<String, CFG>> _poicy2CFGs = new HashMap<String, Map<String, CFG>>();
	private Map<String, Set<String>> _policy2ConcernedEvts = new HashMap<>();
	private Map<String, HashSet<String>> abs2ConcreteAct;

	public StaticAnalysis(Policy[] policies, String path) { this(policies, path, null);}
	public StaticAnalysis(Policy[] policies, String path, String path4absActFiles) {
		// step 1: get declared policy names (e.g., edu.cseusf.poco.poco_demo.examplePolicies.P_confirm)
		_policyNames = getPolicyNames(policies);
		
		// step 2: get information about abstract actions
		abs2ConcreteAct = ExtractAbsActSigs.extract(path4absActFiles);
		
		// step 3: get declared policy source files
		Map<String, PolicyScanner> policy2scanner = new HashMap<String, PolicyScanner>();
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		ArrayList<JavaFileObject> policyFiles = scanPolicyFles(compiler, abs2ConcreteAct, path, policy2scanner);

		// step 4: compile policies
		File[] clzFiles = compilePolicies(compiler,policyFiles,path);
		
		// step 5: analyze policy
		analyzePolicies(policy2scanner, clzFiles);
	}
	
	private ArrayList<JavaFileObject> scanPolicyFles(JavaCompiler compiler, Map<String, HashSet<String>> abs2Conc, String path,
			 Map<String, PolicyScanner> policy2scanner) {
		// step 1: load all java source files
		File[] files = loadFiles(path, ".java");
		StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null);
		JavacTask task = (JavacTask) compiler.getTask(null, fm, null, null, null, fm.getJavaFileObjects(files));
		ArrayList<JavaFileObject> policyFiles = new ArrayList<>();
		
		try {
			PolicyScanner.setDeclaredPolicies(_policyNames);
			for(CompilationUnitTree ct: task.parse()) {
				// 1. scanner the source code file
				PolicyScanner currPolicy = new PolicyScanner(abs2Conc);
				ct.accept(currPolicy, null);
				// 2. skip non-policy files and undeclared policy files
				if( !currPolicy.isPoCoPolicy())
					continue;
				
				policyFiles.add(ct.getSourceFile());
				policy2scanner.put(currPolicy.get_className(), currPolicy);
			}
		} catch (Exception e) { System.err.println("Cannot read source-code files, please check"); System.exit(-1);}
		
		return policyFiles;
	}
	
	private File[] compilePolicies(JavaCompiler compiler, ArrayList<JavaFileObject> cus, String path) {
		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
			CompilationTask task = compiler.getTask(null, fileManager, null, null, null, cus);
			if (task.call())  {
				List<File> clzFiles = Arrays.asList(loadFiles(path, ".class"));
				String clzPath =  createFolder4Clz(path);
				clzFiles.forEach(item->item.renameTo(new File(clzPath + "/"+item.getName()) ));
				return loadFiles(clzPath, ".class");
			}else {
				compilationFailure();
			}
			
		} catch (Exception e) {compilationFailure(); }
		return null;
	}
	
	private ArrayList<String> getPolicyNames(Policy[] policies) {
		ArrayList<String> names = new ArrayList<>();
		if(policies != null && policies.length>0)
			for(Policy p: policies)  
				if(p != null)  names.add(p.getClass().getSimpleName());
		return names;
	}
	
	private File[] loadFiles(String path, String ext) {
		File folder4SourceFiles = new File(path);
		return folder4SourceFiles.listFiles((dir, name) -> { return name.endsWith(ext);});
	}

	private void compilationFailure() {
		System.out.println("Compilation failed");
		System.exit(-1);
	}
	
	private void analyzePolicies(Map<String, PolicyScanner> policy2scanner, File[] clzFiles) {
		for(String policy: policy2scanner.keySet()) {
			PolicyScanner ps = policy2scanner.get(policy);
			Map<String, ArrayList<EventInfo>> mtd2evtSigs = ps.getMthName2Evtsigs();

			// 1. analyze Class Files
			PolicyVisitor pv = analyzeClassFiles(getClzFiles(clzFiles, policy));
			Map<String, ArrayList<String>> mtd2evtDetailedSigs = pv.getMthName2Evtsigs();
			
			// 2. collection info about policy's security relevant events
			_policy2ConcernedEvts.put(pv.getClzName(), ps.getSecReleEvts());
			
			// 3. merge and generate CFG for SL and FL
			mergeInfo(mtd2evtSigs, mtd2evtDetailedSigs);

			// 4. generate CFG
			genCFGs(pv.getClzName(), mtd2evtSigs);
		}
		
		// 5. output warning for possible conflict between policies and obligations
		Set<String> policies = _poicy2CFGs.keySet();
		for (String policy : policies) {
			Map<String, CFG> mtdCFGs = _poicy2CFGs.get(policy);
			for (String  mtd : mtdCFGs.keySet()) 
				preValidation(policy, mtdCFGs.get(mtd));
		}
	}

	private File getClzFiles(File[] clzFiles, String name) {
		if(clzFiles == null || clzFiles.length == 0 || name == null || name.length() ==0)
			return null;
		
		for(File file: clzFiles)  
			if(FilenameUtils.removeExtension(file.getName()).equals(name))
				return file;
		
		return null;
	}
	private void genCFGs(String policyName, Map<String, ArrayList<EventInfo>> mtd2evtSigs) {
		Map<String, CFG> mtd2cfgs  = new HashMap<>();
		Set<String> mtds = mtd2evtSigs.keySet();
		mtds.forEach((mtd)->{ mtd2cfgs.put(mtd, GenPolicyCFG.genCFG(mtd2evtSigs.get(mtd), policyName, mtd));});
		_poicy2CFGs.put(policyName, mtd2cfgs);
	}

	private Map<String, ArrayList<EventInfo>> mergeInfo(Map<String, ArrayList<EventInfo>> mtd2evtSigs,
			Map<String, ArrayList<String>> mtd2DetailedEvtSigs) {
		if (mtd2evtSigs == null || mtd2DetailedEvtSigs == null)
			return null;

		Set<String> mtds = mtd2evtSigs.keySet();
		for (String mtd : mtds) {
			ArrayList<EventInfo> evtList = mtd2evtSigs.get(mtd);
			ArrayList<String> detailed = mtd2DetailedEvtSigs.get(mtd);
			merge(mtd, evtList, detailed, mtds);
		}
		return null;
	}

	private void merge(String mtd, ArrayList<EventInfo> evtList, ArrayList<String> detailed, Set<String> mtds) {
		if (evtList.size() == 0) return;

		int index = 0;
		for (int i = 0; i < evtList.size(); i++) {
			String sig = evtList.get(i).getSig();
			if (ParsFlgConsts.IS_STATEMENT_FLAG(sig)) continue;

			for (; index < detailed.size(); index++) {
				// not root
				String longSig = detailed.get(index);
				if (longSig.startsWith("$"))
					longSig = longSig.split("\\$")[1];
				
				longSig = longSig.replaceAll("(null)", "");
				
				if (isMatching(longSig, sig) || isSystemCalls(longSig, sig)) {
					if(sig.equals("setOutput(Result)") || sig.equals("setOutput(Event)") || sig.equals("setOutput(Action)") ) 
						evtList.get(i).setSig("edu.cseusf.poco.policy.Policy.setOutput(Event)");
					else
						evtList.get(i).setSig(longSig);
					index++;
					break;
				} else if (mtds.contains(sig)) {
					evtList.get(i).setSig(longSig);
					index++;
					break;
				} else  {
					//SendEmail.isResolvable(); 
					//edu.cseusf.poco.event.Action.isResolvable(null)
					String mtdName  = RuntimeUtils.getMethodNamewoPackageInfo(sig);
					String longName = RuntimeUtils.getMethodNamewoPackageInfo(longSig);
					
					
					if(longName.equals(mtdName) &&
					   RuntimeUtils.getfunArgstr(sig).equals(RuntimeUtils.getfunArgstr(longSig)) ) {
						String shortPack = RuntimeUtils.getPackageName(sig);
						String longPack  = RuntimeUtils.getPackageName(longSig);
						
						if(abs2ConcreteAct.keySet().contains(shortPack) ) {
							if(longPack.equals("edu.cseusf.poco.event.Action") ||
							   longPack.equals("edu.cseusf.poco.event.Event")) {
								evtList.get(i).setSig(longSig);
								index++;
								break;
							}
						}else if(longPack.equals("java.lang.Object")){
							evtList.get(i).setSig(longSig);
							index++;
							break;
						}else if(shortPack.indexOf('<') > 0 && shortPack.indexOf('>') > 0 && 
							   longPack.contains( shortPack.substring(0, shortPack.indexOf('<'))) ) {
								evtList.get(i).setSig(longSig);
								index++;
								break;
						}
					} 
				}
			}
		}
	}

	private static boolean isSystemCalls(String longSig, String sig) {
		if (sig.startsWith("System.")) {
			switch (sig) {
			case "System.out.print(String)":
			case "System.out.print(java.lang.String)":
			case "System.err.print(String)":
			case "System.err.print(java.lang.String)":
				if (longSig.equals("java.io.PrintStream.print(java.lang.String)"))
					return true;
				
			case "System.out.println(String)":
			case "System.out.println(java.lang.String)":
			case "System.err.println(String)":
			case "System.err.println(java.lang.String)":
				if (longSig.equals("java.io.PrintStream.println(java.lang.String)"))
					return true;
			default:
				break;
			}
		}
		return false;
	}

	private PolicyVisitor analyzeClassFiles(File clzfile) {
		ClassNode cn = new ClassNode(_api);
		ClassReader cr;
		try {
			cr = new ClassReader(new FileInputStream(clzfile));
			cr.accept(cn, ClassReader.EXPAND_FRAMES);

			ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
			PolicyVisitor pv = new PolicyVisitor(_api, cw, cn);
			cr.accept(pv, _api);
			return pv;
		} catch (Exception e) {compilationFailure(); }
		return null;
	}

	private boolean isMatching(String longSig, String sig) {
		String longSig4Mtd = Utils.getMtdName(longSig);
		String shortSig4Mtd = Utils.getMtdName(sig);

		// if (longSig4Mtd.contains(shortSig4Mtd)) {
		if (longSig4Mtd.endsWith(shortSig4Mtd)) {
			String[] longPara = Utils.getMethodParas(longSig);
			String[] shortPara = Utils.getMethodParas(sig);
			if (longPara == null && shortPara == null)
				return true;
			else if (longPara == null || shortPara == null)
				return false;
			else if (longPara.length == shortPara.length)
				return true;
		}
		return false;
	}

	private boolean isRootNode(String sig) {
		return sig != null && sig.equals("RootNode");
	}

	private void preValidation(String currPolicyName, CFG cfg) {
		Set<String> policies = _policy2ConcernedEvts.keySet();
		for (String policy : policies) {
			if (currPolicyName.equals(policy))
				continue;
			check(currPolicyName, cfg, policy);
		}
	}

	private void check(String currPolicyName, CFG cfg, String policy) {
		Set<String> concernedEvts = _policy2ConcernedEvts.get(policy);
		if (concernedEvts.size() == 0)
			return;

		boolean isViolate = false;
		Queue<CFG> queue = new LinkedList<CFG>();
		queue.add(cfg);
		while (!queue.isEmpty() && !isViolate) {
			CFG node = queue.remove();
			if (node.getEvent() != null) {
				String sig = node.getEvent().getSig();
				if (!isRootNode(sig) && !sig.equals("END_OF_METHOD") && !sig.equals("e")) {
					if (matches(concernedEvts, sig)) {
						isViolate = true;
						break;
					}
				}
			}

			if (node.getChildnodes() != null)
				node.getChildnodes().forEach(item -> queue.add(item));
		}
		if (isViolate)
			System.err.println("The policy " + toSimpleName(currPolicyName)
					+ "'s obligations may voildate security concerns of policy " + toSimpleName(policy));
	}

	private String toSimpleName(String pname) {
		if(pname.indexOf('.') !=-1)
			return pname.substring(pname.lastIndexOf('.')+1);
		return pname;
	}
	private static boolean matches(Set<String> concernedEvts, String sig) {
		if (concernedEvts.contains(sig))
			return true;
		else {
			for (String concernedEvt : concernedEvts)
				if (Utils.matchSignature(Utils.validateStr(concernedEvt), sig))
					return true;
		}
		return false;
	}

	private String createFolder4Clz(String path) {
		File clsDir = new File(path.concat("/classFiles"));
		try {
			if (clsDir.exists())
				FileUtils.deleteDirectory(clsDir);
			clsDir.mkdirs();
		} catch (IOException e1) {
			System.err.println("Cannot create folder for class file, please check");
			System.exit(-1); 
		}
		return clsDir.getAbsolutePath();
	}
	
	public Map<String, Map<String, CFG>> getPoicy2CFGs() { return _poicy2CFGs; }
	public ArrayList<String> getPolicyNames() { return _policyNames;}
	public void setPolicyNames(ArrayList<String> names) { _policyNames = names;}
}