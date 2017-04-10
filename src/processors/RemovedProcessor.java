package processors;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import main.Peer;

public class RemovedProcessor implements Runnable {
	private Peer peer;
	private DatagramPacket packet;
	private String[] headerAttributes;
	private boolean initiateBackupProtocol;
	private int CHUNK_SIZE = 64000;
	
	public RemovedProcessor(Peer peer, DatagramPacket packet) {
		this.peer = peer;
		this.packet = packet;
		this.initiateBackupProtocol = true;
		
		if(!this.peer.getPendentProcesses().containsKey("RemovedBackup"))
			this.peer.getPendentProcesses().put("RemovedBackup", new ConcurrentHashMap<String, Runnable>());
	}

	@Override
	public void run() {
		processRequest();
		
		String fileID = this.headerAttributes[3];
		String chunkNo = this.headerAttributes[4];
		
		if(!this.headerAttributes[2].equals(this.peer.getPeerID())) {
			
			String storedMessage = simulateStoredMessage();
			this.peer.getRecords().get(fileID).getChunks().get(chunkNo).getStoredMessages().remove(storedMessage);
			
			
			if(this.peer.getRecords().get(fileID).getChunks().get(chunkNo).getStoredMessages().size() < 
					this.peer.getRecords().get(fileID).getReplicationDegree()) {
				
				
				if(!this.peer.getPendentProcesses().get("RemovedBackup").containsKey(fileID+chunkNo))
					this.peer.getPendentProcesses().get("RemovedBackup").put(fileID+chunkNo, this);
				
				chunkBackup(fileID, chunkNo);
								
				this.peer.getPendentProcesses().get("RemovedBackup").remove(fileID+chunkNo);
			}			
		}
	}
	
	public void chunkBackup(String fileID, String chunkNo) {
		int randomWait = new Random().nextInt(401);
		
		try {
			Thread.sleep((long)randomWait);
			
			if(this.initiateBackupProtocol) {
				
				int replicationDegree = this.peer.getRecords().get(fileID).getReplicationDegree();
				String filePath = this.peer.getFolderPath() + "\\(" + fileID + "," + chunkNo + ")";
				FileInputStream reader = new FileInputStream(filePath);
				
				byte[] chunkRead = new byte[CHUNK_SIZE];
				
				
				int n = reader.read(chunkRead);
				byte[] chunk = Arrays.copyOfRange(chunkRead, 0, n);
				
				reader.close();
				
				System.out.println("Initialized backup protocol for: ");
				System.out.println("FileID: " + fileID + " Chunk #" + chunkNo);
				
				this.peer.getProcessorsQueue().put(new PutChunkSender(this.peer, chunk, fileID, Integer.parseInt(chunkNo), replicationDegree));
			}
		} 
		
		catch (InterruptedException e) { e.printStackTrace(); } 
		catch (FileNotFoundException e) { System.out.println("The removed chunk is not stored in this peer!"); } 
		catch (IOException e) { e.printStackTrace(); }
				
	}
	
	public void processRequest() {
		String endHeader = "" + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
		String header = new String(Arrays.copyOfRange(packet.getData(), 0, new String(packet.getData()).indexOf(endHeader)));
		
		System.out.println(header);
		
		this.headerAttributes = header.split(" +");
	}
	
	public String simulateStoredMessage() {
		return ("STORED " + this.headerAttributes[1] + " " + this.headerAttributes[2] + " " + this.headerAttributes[3] + " " + 
				this.headerAttributes[4]);
	}
	
	public boolean getInitiateBackupProtocol() { return this.initiateBackupProtocol; }
	public void setInitiateBackupProtocol(boolean bol) { this.initiateBackupProtocol = bol; }
}
