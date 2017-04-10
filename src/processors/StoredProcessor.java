package processors;

import java.net.DatagramPacket;
import java.util.Arrays;

import main.Peer;

public class StoredProcessor implements Runnable {
	
	private Peer peer;
	private DatagramPacket packet;
	private String endHeader = "" + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
	
	public StoredProcessor(Peer peer, DatagramPacket packet) {
		
		this.peer = peer;
		this.packet = packet;
	}

	@Override
	public void run() {
		
		String answer = new String(Arrays.copyOfRange(this.packet.getData(), 0, new String(this.packet.getData()).indexOf(endHeader)));
		String[] headerAttr = answer.split(" +");
		
		String fileID = headerAttr[3];
		String chunkNo = headerAttr[4];
		
		peer.getRecords().get(fileID).getChunks().get(chunkNo).addStoredMessage(answer);
		
		System.out.println("<MC> MC: " + answer);
	}

}
