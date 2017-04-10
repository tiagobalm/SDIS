package listeners;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

import main.Peer;
import processors.ChunkProcessor;

public class MDRListener extends Thread {

	private MulticastSocket MDRSocket;
	private Peer peer;
	private int REQUEST_SIZE = 64300;
	private String endHeader = "" + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
	
	public MDRListener(Peer peer) {
		
		try {
			
			this.MDRSocket = new MulticastSocket(peer.getMDRPort());
			this.MDRSocket.joinGroup(peer.getMDRIPAddress());
			
			this.peer = peer;
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sortMessages(DatagramPacket packet) {
		String header = new String(Arrays.copyOfRange(packet.getData(), 0, new String(packet.getData()).indexOf(endHeader)));
		String[] headerAttributes = header.split(" +");
		
		try {
			switch(headerAttributes[0]) {
			case "CHUNK":
				this.peer.getProcessorsQueue().put(new ChunkProcessor(peer, packet));
				break;
			default:
				System.out.println("Invalid Message.");
				break;
			}
		} catch (InterruptedException e) { e.printStackTrace(); }
	}
	
	public void run() {
		
		while(true) {
			
			byte[] request = new byte[REQUEST_SIZE];
			DatagramPacket requestPacket = new DatagramPacket(request, request.length);
			
			try {
				this.MDRSocket.receive(requestPacket);
				
				sortMessages(requestPacket);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
