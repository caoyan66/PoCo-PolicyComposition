package edu.cseusf.poco.poco_demo.polymerPolicies;

import java.io.File;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.policy.Monitor;
import edu.cseusf.poco.policy.Policy;

public class IsClientSigned extends Policy {
	private boolean haveSelected = false, isFirst = false;

	@Override
	public void onTrigger(Event e) {
		if(!haveSelected)
			checkJarFile(Monitor.getJarFile());
		if (haveSelected && isFirst) 
			setOutput(e);
	}

	private synchronized void checkJarFile(String filePath) {
		JarFile jarfile;
		try {
			jarfile = new JarFile(new File(filePath));
			Enumeration<?> em = jarfile.entries();
			while (em.hasMoreElements()) {
				Certificate[] ca = ((JarEntry) em.nextElement()).getCertificates();
				if (ca != null && ca.length > 0 && ca[0] != null)
					isFirst = true;
			}
			haveSelected = true;
		} catch (Exception e) {
			System.out.println("[IsClientSigned] exception:");
			e.printStackTrace();
			System.exit(1);
		}
	}
}