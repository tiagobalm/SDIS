package listeners;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

import main.Peer;
import processors.*;

public class MDBListener extends Thread {
	
	private MulticastSocket MDBSocket;
	private Peer peer;
	private int PACKET_SIZE = 64300;
	private String endHeader = "" + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
	
	public MDBListener(Peer peer) {
		
		try {
						
			this.MDBSocket = new MulticastSocket(peer.getMDBPort());
			this.MDBSocket.joinGroup(peer.getMDBIPAddress());
			
			this.peer = peer;	
		}
		catch (UnknownHostException e) { e.printStackTrace(); } 
		catch (NumberFormatException e) { e.printStackTrace(); } 
		catch (IOException e) { e.printStackTrace(); }
	}
	
	public void sortMessages(DatagramPacket requestPacket) {
		
		String header = new String(Arrays.copyOfRange(requestPacket.getData(), 0, new String(requestPacket.getData()).indexOf(endHeader)));
		String[] headerAttributes = header.split(" +");
		String command = headerAttributes[0];
		
		try {
			switch(command) {
			case "PUTCHUNK":
				this.peer.getProcessorsQueue().put(new PutChunkReceiver(peer, requestPacket));
				break;
			default:
				System.out.println("Invalid Message.");
				break;
			}
		} catch (InterruptedException e) { e.printStackTrace(); }
	}
	
	public void run() {
		
		while(true) {
			
			byte[] request = new byte[PACKET_SIZE];
			DatagramPacket requestPacket = new DatagramPacket(request, request.length);
			
			try {
				
				this.MDBSocket.receive(requestPacket);
				sortMessages(requestPacket);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
