package edu.cseusf.poco.poco_demo;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import edu.cseusf.poco.poco_demo.polymerPolicies.AllowOnlyMIME;
import edu.cseusf.poco.poco_demo.polymerPolicies.Attachments;
import edu.cseusf.poco.poco_demo.polymerPolicies.ClassLoaders;
import edu.cseusf.poco.poco_demo.polymerPolicies.ConfirmAndAllowOnlyHTTP;
import edu.cseusf.poco.poco_demo.polymerPolicies.DisSysCalls;
import edu.cseusf.poco.poco_demo.polymerPolicies.IncomingEmail;
import edu.cseusf.poco.poco_demo.polymerPolicies.InterruptToCheckMem;
import edu.cseusf.poco.poco_demo.polymerPolicies.IsClientSigned;
import edu.cseusf.poco.poco_demo.polymerPolicies.NoOpenClassFiles;
import edu.cseusf.poco.poco_demo.polymerPolicies.OutgoingMail;
import edu.cseusf.poco.poco_demo.polymerPolicies.Reflection;
import edu.cseusf.poco.policy.Monitor;
import edu.cseusf.poco.policy.Policy;
import edu.cseusf.poco.policy.runtime.ImportPolicy;

public class PoCoDemo {
	private static Monitor pocoPolicy;
	  
	public static void main(String[] args) { 
		if (args.length != 2 || !args[0].endsWith(".jar")) {
		      System.out.println("please enter the targetJarFile and points to a jar file containing the appClassWithMain class file");
		      System.exit(1);
		}
		String path = Paths.get("").toAbsolutePath().toString();
		String path4SourceFiles = path.concat("/src/main/java/edu/cseusf/poco/poco_demo/polymerPolicies");
		String path4absActFiles = path4SourceFiles.concat("/absActions");
		Policy allowOnlyMIME  = new AllowOnlyMIME();
		Policy attachments    = new Attachments();
		Policy classLoaders   = new ClassLoaders();
		Policy incomingMail   = new IncomingEmail();
		Policy outgoingMail   = new OutgoingMail();
		Policy disSyscalls    = new DisSysCalls();
		Policy clientSigned = new IsClientSigned();
		Policy noOpenClassFiles = new NoOpenClassFiles();
		Policy reflection = new Reflection();
		Policy checkMem = new InterruptToCheckMem(0.1);
		Policy confirmAndAllowOnlyHTTP = new ConfirmAndAllowOnlyHTTP();
		Policy[] policies = {clientSigned, allowOnlyMIME, confirmAndAllowOnlyHTTP, incomingMail, outgoingMail, 
				             classLoaders, attachments, noOpenClassFiles, disSyscalls, checkMem, reflection};
		pocoPolicy = new Monitor(args[0], policies, path4SourceFiles, path4absActFiles);

		File myJar = new File(args[0]);
		try {
			URLClassLoader loader = new URLClassLoader(new URL[] { myJar.toURI().toURL() });
			Class<?> clzz = loader.loadClass(args[1]);
			Method mainMtd = clzz.getDeclaredMethod("main", String[].class);
			String[] params = null;
			mainMtd.invoke(null, (Object) params);
			loader.close();
		} catch (Exception e) { e.printStackTrace(); }
	}

	public static Monitor getPocoPolicy() {
		return pocoPolicy;
	}
}