package listeners;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

import main.Peer;
import processors.*;

public class MCListener extends Thread {
	
	private MulticastSocket MCSocket;
	private Peer peer;
	private int REQUEST_SIZE = 300;
	private String endHeader = "" + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
	
	public MCListener(Peer peer) {
		
		try {
			
			this.MCSocket = new MulticastSocket(peer.getMCPort());
			this.MCSocket.joinGroup(peer.getMCIPAddress());
			
			this.peer = peer;
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sortMessages(DatagramPacket request) {
		
		String header = new String(Arrays.copyOfRange(request.getData(), 0, new String(request.getData()).indexOf(endHeader)));
		String[] headerAttributes = header.split(" +");
		String command = headerAttributes[0];
		
		try {
			switch(command) {
			case "STORED":
				this.peer.getProcessorsQueue().put(new StoredProcessor(peer, request));
				break;
			case "GETCHUNK":
				this.peer.getProcessorsQueue().put(new GetChunkProcessor(peer, request));
				break;
			case "DELETE":
				this.peer.getProcessorsQueue().put(new DeleteReceiverProcessor(peer, request));
				break;
			case "REMOVED":
				this.peer.getProcessorsQueue().put(new RemovedProcessor(peer, request));
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
				
				this.MCSocket.receive(requestPacket);
				sortMessages(requestPacket);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
