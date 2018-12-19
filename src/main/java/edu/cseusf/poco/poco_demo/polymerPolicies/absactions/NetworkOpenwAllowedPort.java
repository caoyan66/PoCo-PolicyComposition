package edu.cseusf.poco.poco_demo.polymerPolicies.absactions;

import java.util.ArrayList;
import java.util.Arrays;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;

public class NetworkOpenwAllowedPort extends AbsAction {
	private ArrayList<Integer> allowedPorts;
	
	public NetworkOpenwAllowedPort(Integer[] ports) {
		allowedPorts = new ArrayList<Integer>(Arrays.asList(ports));
	}

	public boolean mapConc2Abs(Action conc) {
		if (conc.matches(new Action("java.net.Socket.<init>(java.lang.String,int,*)"))             			||
		    conc.matches(new Action("java.net.Socket.<init>(java.net.InetAddress,int,*)")) 		  			||
		    conc.matches( new Action("java.net.DatagramSocket.send(java.net.DatagramPacket)") )  			||
		    conc.matches(new Action("java.net.MulticastSocket.joinGroup(java.net.InetAddress)")) 			||
		    conc.matches(new Action("java.net.MulticastSocket.leaveGroup(java.net.InetAddress)"))			||
		    conc.matches( new Action("java.net.MulticastSocket.send(java.net.DatagramPacket,*)")) 			||
		    conc.matches(new Action("javax.mail.Service.protocolConnect(java.lang.String,int,*)")) 			||
			conc.matches(new Action("com.sun.mail.imap.IMAPStore.protocolConnect(java.lang.String,int,*)"))	||
		    conc.matches(new Action("com.sun.mail.pop3.POP3Store.protocolConnect(java.lang.String,int,*)"))  ||
			conc.matches(new Action("com.sun.mail.smtp.SMTPTransport.protocolConnect(java.lang.String,int,*)"))) {
			
			AbsAction networkOpen = new NetworkOpen();
			
			if(networkOpen.mapConc2Abs(conc) ) {
				_matchingInfo = networkOpen.getEvtInfo();
				if( networkOpen.isResolvable() ) {
					return _matchingInfo != null 	&& 
						   _matchingInfo[0] != null && 
						   allowedPorts.contains((Integer) _matchingInfo[0]);
				}else {
					this._resolvable = false;
					return true;
				}
			}
		}
		 
		return false;
	}
}
