package edu.cseusf.poco.poco_demo.polymerPolicies;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JOptionPane;

import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.policy.Policy;

public class InterruptToCheckMem extends Policy {
	private boolean havePopped = false;
	private boolean activated = false;
	
	public void onTrigger(Event e) {}

	public InterruptToCheckMem(double maxPercent) {
		if(!activated) {
    		activated = true;
    		new Timer().schedule(new TimerTask() {
    			public void run() {
    				Runtime r = Runtime.getRuntime();
    				while (true) {
    					try {
    						Thread.sleep(2000);
    						if (!havePopped && r.totalMemory() / r.maxMemory() > maxPercent) {
    							havePopped = true;
    							String msg = "More than " + maxPercent + "% of the memory available to the VM has been consumed";
    							JOptionPane.showMessageDialog(null, msg, "Warning", 0);
    						}
    					} catch (InterruptedException e) { }
    				}
			} }, 10000, 3000);
		}
	}
}