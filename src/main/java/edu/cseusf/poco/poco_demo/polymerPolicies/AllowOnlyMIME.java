package edu.cseusf.poco.poco_demo.polymerPolicies;
import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.NetworkOpen;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.NetworkOpenwAllowedPort;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;

/**
 * The AllowOnlyMIME policy restricts the ports that 
 * a network socket can be opened on.    Because the 
 * application  is  an  email client, it should only
 * communicate o ver the network on  ports that  are 
 * associated  with email  protocols. The IMAP, SMTP, 
 * and POP3  protocols use  ports 143, 25,  and  110,
 * respectively. IMAP and POP3 transmissions can  be 
 * secured using SSL, in which case IMAP  uses  port 
 * 993, SMTP uses port 587 and POP3 uses port 995.
 * 
 * @author yan
 *
 */
public class AllowOnlyMIME extends Policy {
	private Integer[] mimePorts = new Integer[]{143, 993, 25, 587, 110, 995};
	private Action openNetwork = new NetworkOpen();
	private Action openMIME = new NetworkOpenwAllowedPort(mimePorts);
	
	public void onTrigger(Event e) {
		if( e.isAction() && e.matches(openNetwork) ) {
			if(e.matches(openMIME)) 
				setOutput(e);
			else
				setOutput( new Result(e, null) );
		}
	}
	
	public boolean vote(CFG cfg) {
		return  !cfg.containsUnresolved(openNetwork) &&
				!(cfg.evtCounts(openNetwork) > cfg.evtCounts(openMIME)) &&
			    !cfg.outputSets(openNetwork);
	}
}  