package edu.cseusf.poco.policy.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import edu.cseusf.poco.policy.staticAnalysis.ExtractAbsActSigs;

public class ImportPolicy {
	/**
	 * The ImportPolicy program converts the candidate policy files into
	 * policy-enforcement code in Java.
	 *               
	 * @param args { args[0]:the absolute path of all the specified PoCo files;
	 *           	 args[1]:the desired output path for converted PoCo files;
	 *           	 args[2]:the desired package name; 
	 *               args[3]:the absolute path of abstract action files}
	 */
	public static void importPolicies(String[] args) {
		// 1. check argument
		if (args.length < 4) {
			System.err.println("Please input the absolute path of candidate PoCo policies.");
			System.exit(-1);
		}
		
		// 2. load candidate policy files
		File[] pfiles = getPolicyFiles(args[0], ".pol");
		String outputPath = args[1];
		
		// 3. handle Absaction
		Map<String, HashSet<String>> abs2Conc = new HashMap<>();
		abs2Conc = handleAbsaction(args[0], outputPath, args[2]);
		
		// 4. collection security relevant events info
		String[] pnames = new String[pfiles.length];
		File[] newpfiles = new File[pfiles.length];
		
		for (int i = 0; i < pfiles.length; i++) {
			pnames[i] = FilenameUtils.getBaseName(pfiles[i].getName());
			newpfiles[i] = copyFile(pfiles[i], outputPath+"/policies", args[2]);
		}
		Set<String> secRelEvts = ExtractAbsActSigs.parsePolicyFiles(newpfiles, abs2Conc);
		
		//5. load candidate os, and vc if specified
		pfiles = loadFiles(args[0], ".os");
		if(pfiles!= null && pfiles.length ==1)
			copyFile(pfiles[0], outputPath, args[2]);
		
		pfiles = loadFiles(args[0], ".vc");
		if(pfiles!= null && pfiles.length ==1)
			copyFile(pfiles[0], outputPath, args[2]);
		
		//6. generate root policy
		genRootPolicy(pnames, outputPath,args[2]);
		
		// 7. generate AspectJ file
		new GenAspectJFile().gen(args[2], secRelEvts, outputPath);
	}
	
	private static void genRootPolicy(String[] pnames, String outputPath, String packInfo) {
		File newfile 		= null;
		boolean success 	= true;
		BufferedReader in 	= null;
		PrintWriter out 	= null;

		if(outputPath == null || outputPath.equals(""))
			outputPath = Paths.get("").toAbsolutePath().toString();
		File dir = new File(outputPath);
		dir.mkdirs();

		try {
			newfile = new File(dir, "PoCoDemo.java");
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			InputStream is = classloader.getResourceAsStream("rootPolicy.txt");
			in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			out = new PrintWriter(new FileWriter(newfile));
			
			String line = null;
			while ((line = in.readLine()) != null) {
				if(line.equals("##import pocoPolies##")) {
					if(packInfo!=null && packInfo.length() >0)
						out.println("import " + packInfo + ".*;\n");
				}
				else if(line.equals("##declare pocoPolies##")) {
					for (int i=0; i< pnames.length; i++) 
						out.println("\t\tPolicy policy"+ i + " = new "+pnames[i]+"();");
					 
					out.print("\t\tPolicy[] policies = {");
					for (int i=0; i< pnames.length; i++) {
						out.print("policy"+i);
						if(i != pnames.length -1)
							out.print(",");
					}
					out.print("};\n");
				}else  {
					out.println(line);
				}
			}
		} catch (IOException e) {
			success = false;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}

		if (!success) {
			System.err.println("<PoCo> Error1: cannot read the root policy template file!");
			System.exit(-1);
		}
	}
	
	private static File[] getPolicyFiles(String path, String ext) {
		File[] pfiles = loadFiles(path, ext);
		if (pfiles.length == 0) {
			System.err.println("<PoCo> Error2: cannot read the policy file!");
			System.exit(-1);
		}
		return pfiles;
	}
	
	private static Map<String, HashSet<String>> handleAbsaction(String absActPath, String outputPath, String packInfo) {
		Map<String, HashSet<String>> abs2ConcreteAct = new HashMap<>();
		if(absActPath != null) {
			File[] files = loadFiles(absActPath + "/absactions", ".action");
			if (files.length > 0) {
				// handle abstract actions if defined
				File[] newfiles = new File[files.length];
				for (int i = 0; i < files.length; i++)
					newfiles[i] = copyFile(files[i], outputPath + "/absactions", packInfo);

				// get information about abstract actions
				abs2ConcreteAct = ExtractAbsActSigs.parseAbsActionFiles(newfiles);
			}
		}
		genab2conc(abs2ConcreteAct, outputPath);
		return abs2ConcreteAct;
	}

	private static void genab2conc(Map<String, HashSet<String>> abs2ConcreteAct, String outputPath) {
		File newfile = null;
		boolean success = true;
		PrintWriter out = null;

		if(outputPath == null || outputPath.equals(""))
			outputPath = Paths.get("").toAbsolutePath().toString();
		File dir = new File(outputPath);
		dir.mkdirs();

		try {
			newfile = new File(dir, "Abs2ConcreteAct.java");
			out = new PrintWriter(new FileWriter(newfile));
			
			out.println("public class Abs2ConcreteAct {\n");
			out.println("\tpublic static String[] getConcActs(String absAct) {");
			out.println("\t\tswitch (absAct) {");
			
			if(abs2ConcreteAct!= null && abs2ConcreteAct.keySet().size()>0) {
				Set<String> keys = abs2ConcreteAct.keySet();
				for(String key: keys) {
					out.println("\t\t\tcase \""+key+"\":");
					out.println("\t\t\t\treturn "+ key.toLowerCase()+"1;");
				}
			}
			out.println("\t\t\tdefault: return null;");
			out.println("\t\t}");
			out.println("\t}");
			
			if(abs2ConcreteAct!= null && abs2ConcreteAct.keySet().size()>0) {
				Set<String> keys = abs2ConcreteAct.keySet();
				for(String key: keys) {
					out.println("\tprivate static String[] " + key.toLowerCase()+"1 = {");
					int i = 0, len = abs2ConcreteAct.get(key).size();
					for(String val:abs2ConcreteAct.get(key)) {
						out.print("\t\t\t\""+val+"\"");
						if(i++ != len-1)
							out.println(",");
						else 
							out.println("};\n");
					}
				}
			}
			out.println("}");
		} catch (IOException e) {
			success = false;
		} finally { IOUtils.closeQuietly(out); }

		if (!success) {
			System.err.println("<PoCo> Error3: cannot read the root policy template file!");
			System.exit(-1);
		}
	}
	
	private static File copyFile(File f, String outputPath, String packInfo) {
		File newfile = null;
		boolean success = true;
		BufferedReader in = null;
		PrintWriter out = null;

		File dir = new File(Paths.get("").toAbsolutePath().toString());
		if (!outputPath.equals("")) {
			dir = new File(outputPath);
			dir.mkdirs();
		}

		try {
			String fileName = FilenameUtils.getBaseName(f.getName());
			newfile = new File(dir, fileName + ".java");

			in = new BufferedReader(new FileReader(f));
			out = new PrintWriter(new FileWriter(newfile));
			
			if(packInfo != null)  out.println("package " + packInfo + ";\n");
			out.println("import edu.cseusf.poco.event.*;");
			out.println("import edu.cseusf.poco.policy.*;\n"); 
			
			String line = null;
			while ((line = in.readLine()) != null)
				out.println(line);
		} catch (IOException e) {
			success = false;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}

		if (!success) {
			System.err.println("<PoCo> Error4: cannot read the candidate policy files!");
			System.exit(-1);
		}
		return newfile;
	}

	private static File[] loadFiles(String path, String ext) {
		File folder4SourceFiles = new File(path);
		return folder4SourceFiles.listFiles((dir, name) -> {
			return name.endsWith(ext);
		});
	}
}