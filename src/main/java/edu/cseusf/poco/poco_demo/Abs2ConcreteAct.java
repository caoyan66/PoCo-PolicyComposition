package edu.cseusf.poco.poco_demo;

public class Abs2ConcreteAct {
	
	public static String[] getConcActs(String absAct) {
		switch (absAct) {
		case "FileOpen":
		case "OpenDangerousFile": 	return fileOpen;
		case "NetworkOpen": 		return networkOpen;
		case "ReceiveEmail": 		return receiveEmail;
		case "SendEmail": 			return sendEmail;
		case "FileWrite": 			return fileWrite;
		default: return null;
		}
	}
	
	private static String[] fileOpen = {"java.io.FileInputStream.<init>(java.io.File)", 
										"java.io.FileWriter.<init>(java.io.File)",
										"java.io.FileWriter.<init>(java.io.File,boolean)",
										"java.io.FileWriter.<init>(java.lang.String)",
										"java.io.FileWriter.<init>(java.lang.String,boolean)",
										"java.io.FileInputStream.<init>(java.lang.String)",
										"java.io.RandomAccessFile.<init>(java.lang.String,java.lang.String)",
										"java.util.zip.ZipFile.<init>(java.lang.String)",
										"java.io.RandomAccessFile.<init>(java.io.File,java.lang.String)"};
	
	private static String[] networkOpen = {"javax.mail.Service.protocolConnect(java.lang.String,int, ..)",             
										   "java.net.Socket.<init>(java.lang.String,int,..)",
										   "java.net.Socket.<init>(java.net.InetAddress,int,..)",
										   "java.net.ServerSocket.<init>(int)",
										   "com.sun.mail.imap.IMAPStore.protocolConnect(java.lang.String,int,..)",
										   "com.sun.mail.pop3.POP3Store.protocolConnect(java.lang.String,int,..)",
										   "com.sun.mail.smtp.SMTPTransport.protocolConnect(java.lang.String,int,..)",
										   "java.net.DatagramSocket.send(java.net.DatagramPacket)",
										   "java.net.MulticastSocket.send(java.net.DatagramPacket,..)",
										   "java.net.MulticastSocket.joinGroup(java.net.InetAddress)",
										   "java.net.MulticastSocket.leaveGroup(java.net.InetAddress)"};
	
	private static String[] receiveEmail = {"javax.mail.internet.MimeMessage.getSubject()",             
			   								"com.sun.mail.imap.IMAPMessage.getSubject()",
			                                "javax.mail.internet.MimeMessage.getContent()"};
		
	private static String[] sendEmail = {"javax.mail.Transport.send(javax.mail.Message)",             
										 "javax.mail.Transport.send(javax.mail.Message,*)",
										 "com.sun.mail.smtp.SMTPTransport.sendMessage(javax.mail.Message,*)"};
	
	private static String[] fileWrite = {"java.io.File.createTempFile(java.lang.String,java.lang.String,..)",             
			 							 "java.io.File.renameTo(java.io.File)",
										 "java.io.FileOutputStream.<init>(java.io.File)",
										 "java.io.File.createNewFile()",
										 "java.io.FileOutputStream.<init>(java.lang.String,..)",
										 "java.io.RandomAccessFile.<init>(java.lang.String,java.lang.String)",
										 "java.io.RandomAccessFile.<init>(java.io.File,java.lang.String)"};
	
}