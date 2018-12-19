package edu.cseusf.poco.policy.staticAnalysis;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;

public class ExtractAbsActSigs {
	public static Map<String, HashSet<String>> extract(String folderPath) {
		// 1. get all the signatures
		Map<String, HashSet<String>> res =  new HashMap<String, HashSet<String>>();
		if(folderPath == null || folderPath.equals("")) return res; 
		AbsActionVisitor myVisit = new AbsActionVisitor();
		parseAbsActionFiles(getAbsActionFiles(folderPath), myVisit);
		return myVisit.getAbsName2Sigs();
	}

	public static Map<String, HashSet<String>> parseAbsActionFiles(File[] file) {
		AbsActionVisitor myVisit = new AbsActionVisitor();
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try (StandardJavaFileManager fileManager = compiler
				.getStandardFileManager(null, null, null)) {
			JavacTask task = (JavacTask) compiler.getTask(null, fileManager,
					null, null, null, fileManager.getJavaFileObjects(file));
			task.parse().forEach(cu -> cu.accept(myVisit, null));
		} catch (Exception ex) { }
		return  myVisit.getAbsName2Sigs();
	}
	
	public static Set<String> parsePolicyFiles(File[] file, Map<String, HashSet<String>> abs2Conc) {
		PolicyVisitor ps = new PolicyVisitor(abs2Conc);
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try (StandardJavaFileManager fileManager = compiler
				.getStandardFileManager(null, null, null)) {
			JavacTask task = (JavacTask) compiler.getTask(null, fileManager,
					null, null, null, fileManager.getJavaFileObjects(file));
			task.parse().forEach(cu -> cu.accept(ps, null));
		} catch (Exception ex) { }
		return ps.getsecReleEvts();
	}
	
	private static File[] getAbsActionFiles(String folderDir) {
		File folder = new File(folderDir);
		return folder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".java");
			}
		});
	}

	private static void parseAbsActionFiles(File[] files, TreeScanner<Void, Void> scanner) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try (StandardJavaFileManager fileManager = compiler
				.getStandardFileManager(null, null, null)) {
			JavacTask task = (JavacTask) compiler.getTask(null, fileManager,
					null, null, null, fileManager.getJavaFileObjects(files));
			task.parse().forEach(cu -> cu.accept(scanner, null));
		} catch (Exception ex) { }
	}
	 
}