package edu.cseusf.poco.poco_demo.polymerPolicies;

import javax.swing.JOptionPane;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.NetworkOpen;
import edu.cseusf.poco.poco_demo.polymerPolicies.absactions.NetworkOpenwAllowedPort;
import edu.cseusf.poco.policy.CFG;
import edu.cseusf.poco.policy.Policy;


/** Confirms and quietly accepts all HTTP network connections (ports 80 & 443 (SSL)).
 * Suppresses all other network connections.
 * A message is output to the screen every time a connection is made.
 */

public class ConfirmAndAllowOnlyHTTP extends Policy {
	private Action openNetwork = new NetworkOpen();
	private final Integer[] httpPorts = new Integer[]{80, 433};
	private Action openhttp = new NetworkOpenwAllowedPort(httpPorts);
	private boolean userCancel = false, noAsk = false;

	public void onTrigger(Event e){
		if( e.isAction() && e.matches(openNetwork) && outputNotSet() ) {
			if(e.matches(openhttp)) {
				if(noAsk) 		{ setOutput(e); 					return;}
				if(userCancel) 	{ setOutput( new Result(e, null) ); return;}
				
				String msg = "The program is attempting to make an HTTP connection\n" +  "to :"+
						  openhttp.getEvtInfo()[0].toString()+".\nAllow this connection?";
				
				if( JOptionPane.showConfirmDialog(null, msg, "Security Question",0) == 0 )  {
					noAsk = true;		setOutput(e);
				}else {
					userCancel = true; 	setOutput( new Result(e, null) );
				}
			}
			else
				setOutput( new Result(e, null) );
		}
	}
	
	public boolean vote(CFG cfg) { 
		return  !(cfg.evtCounts(openNetwork) > cfg.evtCounts(openhttp)) &&
			    !cfg.outputSets(openNetwork);
	}
}
