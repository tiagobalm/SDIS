package processors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.TimeUnit;

import main.Peer;
import utilities.State;

public class PutChunkSender implements Runnable {
	private Peer peer;
	private State state;
	private String fileID;
	private int chunkCounter, replicationDegree;
	private DatagramPacket packetToSend;
	private int numberOfTries;
	private MulticastSocket MDBSocket;
	
	public PutChunkSender(Peer peer, byte[] chunk, String fileID, int chunkCounter, int replicationDegree) {
		this.peer = peer;
		this.fileID = fileID;
		this.chunkCounter = chunkCounter;
		this.replicationDegree = replicationDegree;
		this.packetToSend = createDatagramPacket(chunk);
		this.state = State.SEND;
		this.numberOfTries = 0;
		
		try {
			MDBSocket = new MulticastSocket(peer.getMDBPort());
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	
	public DatagramPacket createDatagramPacket(byte[] chunk) {
		String header = createHeader();
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		
		try {
			data.write(header.getBytes());
			data.write(chunk);			
		} catch (IOException e) { e.printStackTrace(); }
		
		/* Creates a record for the chunk, if it does not exist*/
		peer.getRecords().get(fileID).addChunk(String.valueOf(chunkCounter), chunk.length);
		
		return new DatagramPacket(data.toByteArray(), data.toByteArray().length, peer.getMDBIPAddress(), peer.getMDBPort());
	}
	
	public String createHeader() {
		return "PUTCHUNK " + peer.getProtocolVersion() + " " + peer.getPeerID() + " " + fileID 
				+ " " + chunkCounter + " " + replicationDegree + (char)0x0D + (char)0x0A + (char)0x0D 
				+ (char)0x0A;
	}
	
	public void sendPacket() {
		
		try {
			MDBSocket.send(packetToSend);
		} catch (IOException e) { e.printStackTrace(); }
		
		this.state = State.CHECKREPLICATION;
		this.numberOfTries++;
		System.out.println("Try #" + this.numberOfTries + " for:");
		System.out.println("FileID: " + this.fileID + " Chunk #" + this.chunkCounter);
		peer.getExecutor().schedule(this, (long)Math.pow(2, this.numberOfTries - 1), TimeUnit.SECONDS);
	}
	
	public void checkReplicationDegree() {
				
		if(this.numberOfTries < 5 && 
				peer.getRecords().get(fileID).getChunks().get(String.valueOf(chunkCounter)).getStoredMessages()
				.size() < replicationDegree) {
			
			try {
				MDBSocket.send(packetToSend);
			} catch (IOException e) { e.printStackTrace(); }
			
			this.numberOfTries++;
			peer.getExecutor().schedule(this, (long)Math.pow(2, this.numberOfTries - 1), TimeUnit.SECONDS);
		}
	}

	@Override
	public void run() {
				
		switch(state) {
		case SEND:
			sendPacket();
			break;
		case CHECKREPLICATION:
			checkReplicationDegree();
			break;
		default:
			break;
		}
	}

}