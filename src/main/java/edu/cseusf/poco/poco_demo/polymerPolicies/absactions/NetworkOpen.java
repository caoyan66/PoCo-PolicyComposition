package edu.cseusf.poco.poco_demo.polymerPolicies.absactions;

import java.net.MulticastSocket;

import edu.cseusf.poco.event.AbsAction;
import edu.cseusf.poco.event.Action;

public class NetworkOpen extends AbsAction {
	
	public boolean mapConc2Abs(Action conc) {
		Integer portNum = null;
		String addr = null;
		
		if (conc.matches(new Action("javax.mail.Service.protocolConnect(java.lang.String,int,*)")) ||
			conc.matches(new Action("java.net.Socket.<init>(java.lang.String,int,*)"))) {
			
			try {
				addr = conc.getArg(0).toString();
				portNum = new Integer(conc.getArg(1).toString());
				if (portNum == -1)	portNum = 587;
			}catch(Exception ex) { _resolvable = false;}
		
		}else if (conc.matches(new Action("java.net.Socket.<init>(java.net.InetAddress,int,*)")) ){
			
			try {
				if(conc.getArg(0) != null)
					addr = ((java.net.InetAddress)conc.getArg(0)).getHostAddress();
				portNum = new Integer(conc.getArg(1).toString());
			}catch(Exception ex) { _resolvable = false;}
		
		}else if (conc.matches(new Action("com.sun.mail.imap.IMAPStore.protocolConnect(java.lang.String,int,*)"))) {
			
			try {
				addr = conc.getArg(0).toString();
				portNum = new Integer(conc.getArg(1).toString());
				if (portNum == -1) portNum = 143;
			}catch(Exception ex) { _resolvable = false;}
		
		}else if (conc.matches(new Action("com.sun.mail.pop3.POP3Store.protocolConnect(java.lang.String,int,*)"))) {
			
			try {
				addr = conc.getArg(0).toString();
				portNum = new Integer(conc.getArg(1).toString());
				if (portNum == -1) portNum = 110;
			}catch(Exception ex) { _resolvable = false;}
		
		} else if (conc.matches(new Action("com.sun.mail.smtp.SMTPTransport.protocolConnect(java.lang.String,int,*)"))) {
		
			try {
				addr = conc.getArg(0).toString();
				portNum = new Integer(conc.getArg(1).toString());
				if (portNum == -1)	portNum = 25;
			}catch(Exception ex) { _resolvable = false;}
		
		} else if (conc.matches( new Action("java.net.DatagramSocket.send(java.net.DatagramPacket)") ) ||
				   conc.matches( new Action("java.net.MulticastSocket.send(java.net.DatagramPacket,*)") )) {
			
			try {
				if(conc.getArg(0) != null) {
					java.net.DatagramPacket temp = (java.net.DatagramPacket)conc.getArg(0);
					if (temp.getAddress() != null) addr = temp.getAddress().getHostAddress();
				}
				if (conc.getArg(1) != null) 
					portNum = ((java.net.DatagramPacket) conc.getArg(1)).getPort();
				
			}catch(Exception ex) { _resolvable = false;}
		
		} else if (conc.matches(new Action("java.net.MulticastSocket.joinGroup(java.net.InetAddress)")) ||
				   conc.matches(new Action("java.net.MulticastSocket.leaveGroup(java.net.InetAddress)"))) {
			
			try {
				if(conc.getArg(0) != null)
					addr = ((java.net.InetAddress)conc.getArg(0)).getHostAddress();
				portNum = ((MulticastSocket) conc.getCaller()).getPort();
			}catch(Exception ex) { _resolvable = false;}
		
		}else {
			return false;
		}
		
		if(portNum == null && addr == null) {
			_matchingInfo = null;
			return !_resolvable;
		}else {
			_matchingInfo = new Object[] {portNum, addr};
			return true;
		}
	} 
	
}