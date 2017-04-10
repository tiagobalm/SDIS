package processors;

import java.net.DatagramPacket;
import java.util.Arrays;

import main.Peer;

public class ChunkProcessor implements Runnable {
	private Peer peer;
	private DatagramPacket packet;
	private byte[] body;
	private String[] headerAttributes;
	
	public ChunkProcessor(Peer peer, DatagramPacket packet) {
		this.peer = peer;
		this.packet = packet;
	}

	@Override
	public void run() {
		
		processPacket();
		
		String fileID = this.headerAttributes[3];
		String chunkNo = this.headerAttributes[4];
				
		if(this.peer.getPendentProcesses().get("Restore").containsKey(fileID + chunkNo))
			((GetChunkProcessor)this.peer.getPendentProcesses().get("Restore").get(fileID + chunkNo)).setSend(false);
			
		else if(this.peer.getPendentProcesses().get("Restore").containsKey(fileID))
			((RestoreProcessor)this.peer.getPendentProcesses().get("Restore").get(fileID))
									.addChunk(Integer.parseInt(chunkNo), this.body);
		else
			System.out.println("Invalid restore object!");
	}
		
	
	public void processPacket() {
		String endHeader = "" + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
		
		String answerHeader = new String(Arrays.copyOfRange(this.packet.getData(), 0, new String(this.packet.getData()).indexOf(endHeader)));
		this.headerAttributes = answerHeader.split(" +");
		
		this.body = Arrays.copyOfRange(this.packet.getData(), new String(this.packet.getData()).indexOf(endHeader)  + endHeader.length(), this.packet.getLength());
	}
}
