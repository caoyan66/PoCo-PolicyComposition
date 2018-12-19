package edu.cseusf.poco.poco_demo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

import edu.cseusf.poco.event.Action;
import edu.cseusf.poco.event.Event;
import edu.cseusf.poco.event.EvtTyp;
import edu.cseusf.poco.event.Result;
import edu.cseusf.poco.policy.Monitor;
import edu.cseusf.poco.policy.Policy;

public aspect PoCoPointCut {
	private long startpgm, b4getContent, duration;
	private Monitor pocoPolicy;
	private Event trigger;
	private Object returnRes;
	
	public PoCoPointCut() { 
		pocoPolicy = PoCoDemo.getPocoPolicy(); 
	}
	pointcut  start(): execution(public static void main(String[])) && within(net.suberic.pooka.Pooka);
	before(): start() { startpgm = System.nanoTime(); }
	
	pointcut doneSetVisible():  call(void net.suberic.pooka.gui.FolderInternalFrame.setVisible(boolean));
	after(): doneSetVisible() {
		duration = (System.nanoTime() - startpgm)/1000;
		try(FileWriter fw = new FileWriter("loadpgm.txt", true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter out = new PrintWriter(bw)) {
				out.println(duration);
		} catch (Exception e) { }
	}
	
	pointcut b4_showMsg(): 
		execution(* net.suberic.pooka.gui.MessageProxy.OpenAction.actionPerformed(java.awt.event.ActionEvent));
	before(): b4_showMsg() {  
		b4getContent = System.nanoTime();
		Monitor.count = 0;}
	pointcut after_showMsg(): call(void net.suberic.pooka.gui.MessageUI.openMessageUI()) && within(net.suberic.pooka.gui.MessageProxy);
	after(): after_showMsg() {
		System.out.println(Monitor.count);
		duration = (System.nanoTime() - b4getContent)/1000;
		try(FileWriter fw = new FileWriter("getMail.txt", true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter out = new PrintWriter(bw)) {
			 	out.println(duration);
		} catch (Exception e) { }
	}
	
	pointcut PointCut0():  
			call( java.net.Socket.new(java.lang.String,int,..)) 			 		||
		    call( java.net.Socket.new(java.net.InetAddress,int,..)) 			 	||
		    call( * java.net.DatagramSocket.send(java.net.DatagramPacket) )     	||
		    call( * java.net.MulticastSocket.send(java.net.DatagramPacket,..) ) 	||
		    call( * java.net.MulticastSocket.joinGroup(java.net.InetAddress) )  	||
		    call( * java.net.MulticastSocket.leaveGroup(java.net.InetAddress) )  	||
		    
		    call( * javax.mail.Folder.getMessage(int)) 								||
		    call( * com.sun.mail.imap.IMAPFolder.expunge()) 						||
		    call( * com.sun.mail.imap.IMAPFolder.fetch(javax.mail.Message[], *)) 	||
		    call( * com.sun.mail.imap.IMAPFolder.getMessage(int)) 					||
		    call( * com.sun.mail.imap.IMAPFolder.getMessageByUID(long)) 			||
		    call( * com.sun.mail.imap.IMAPFolder.getMessagesByUID(*)) 				||
		    call( * com.sun.mail.imap.IMAPFolder.search(..)) 						||
		    call( * com.sun.mail.pop3.POP3Folder.expunge()) 						||
		    call( * com.sun.mail.pop3.POP3Folder.fetch(javax.mail.Message[], *)) 	||
		    call( * com.sun.mail.pop3.POP3Folder.getMessage(int)) 					||

		    call( * javax.mail.Service.protocolConnect(java.lang.String,int, ..) )	||
		    call( * javax.mail.Message.getSubject())								||
		    call( * javax.mail.Message.getContent())								||
		    call( * javax.mail.internet.MimeMessage.getContent())					||
		    call( * javax.mail.internet.MimePart.getContent())						||

		    call( java.io.FileOutputStream.new(java.io.File))						||
		    call( java.io.FileOutputStream.new(java.lang.String,..))				||
		    call( java.io.RandomAccessFile.new(java.lang.String,java.lang.String))	||
		    call( java.io.RandomAccessFile.new(java.io.File,java.lang.String))		||
		    
		    call(java.lang.ClassLoader.new(*))										||
		    
		    call (* java.lang.reflect.Method.invoke(java.lang.Object,java.lang.Object[])) ||
		    call( * javax.mail.Transport.send(javax.mail.Message,..))				||
		    call( * javax.mail.Transport.sendMessage(javax.mail.Message,..) );

	Object around(): PointCut0() {
		if (pocoPolicy.isLocked4Oblig()) {
			Object obj = proceed();
			pocoPolicy.getRtrace().addRes(new Result(thisJoinPoint, obj));
			return obj;
		} else {
			trigger = new Action(thisJoinPoint);
			returnRes = null;
			pocoPolicy.processTrigger(new Action(thisJoinPoint));

			if (Policy.outputNotSet() || trigger.equals(Policy.getOutput())) {
				returnRes = proceed();
			} else {
				Event e = Policy.getOutput();
				if(e.getEventTyp() == EvtTyp.ACTION) {
					returnRes = ((Action) e).execute();
				}else {
					Result result = ((Result) e);
					if (result.getEvtSig() == null && result.getEvtRes() == null) 
						System.exit(-1);
					returnRes = ((Result) e).getEvtRes();
				}
			}
			pocoPolicy.processTrigger(new Result(thisJoinPoint, returnRes));
			if (Policy.outputNotSet()) {
				return returnRes;
			} else {
				Event e = Policy.getOutput();
				if (e == null)
					returnRes = e;
				if (e.getEventTyp() == EvtTyp.RESULT)
					returnRes = ((Result) e).getEvtRes();
				else
					returnRes = ((Action) e).execute();
			}
			return returnRes;
		}
	}

	pointcut PointCut1(): call( * edu.cseusf.poco.policy.Policy+.setOutput(edu.cseusf.poco.event.Event) );

	Object around(): PointCut1() {
		Object obj = proceed();
		pocoPolicy.getRtrace().addRes(new Result("edu.cseusf.poco.policy.Policy.setOutput(edu.cseusf.poco.event.Event)", obj));
		return obj;
	}
}