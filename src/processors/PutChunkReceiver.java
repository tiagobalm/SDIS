package processors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Random;

import main.Peer;
import utilities.FileInfo;

public class PutChunkReceiver implements Runnable {
	
	private DatagramPacket packet;
	private Peer peer;
	private String endHeader = "" + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
	
	public PutChunkReceiver(Peer peer, DatagramPacket packet) {
		this.packet = packet;
		this.peer = peer;
	}

	@Override
	public void run() {
		
		try {
		
			String header = new String(Arrays.copyOfRange(this.packet.getData(), 0, new String(this.packet.getData()).indexOf(endHeader)));
			String[] headerAttr = header.split(" +");
			
			String version = headerAttr[1];
			String senderID = headerAttr[2];
			String fileID = headerAttr[3];
			String chunkNo = headerAttr[4];
			int replicationDegree = Integer.parseInt(headerAttr[5]);
			
			checkForRemovedProcessor(fileID, chunkNo);
												
			if(!senderID.equals(this.peer.getPeerID())) {
				
				if(!this.peer.getRecords().containsKey(fileID) || !this.peer.getRecords().get(fileID).getServerID().equals(this.peer.getPeerID())) {
					
					System.out.println(header);
					
					byte[] body = Arrays.copyOfRange(this.packet.getData(), new String(this.packet.getData()).indexOf(endHeader) + endHeader.length(), packet.getLength());
					
					checkCapacity(body.length);
					
					if(this.peer.getCapacity() + body.length <= this.peer.getMaximumCapacity()) {
						
						storeFile(body, fileID, chunkNo);
						
						storeChunkInfo(fileID, replicationDegree, senderID, chunkNo, body.length);
						
						DatagramPacket answerPacket = buildStoredDatagramPacket(version, fileID, chunkNo);
						
						MulticastSocket MCSocket = new MulticastSocket(peer.getMCPort());
						
						int randomWait = new Random().nextInt(401);
						Thread.sleep((long)randomWait);
						
						MCSocket.send(answerPacket);
						MCSocket.close();
						
						System.out.println("Capacity: " + peer.getCapacity());
					}
				}
			}
		} 
		catch(IOException e) { e.printStackTrace(); } 
		catch (InterruptedException e) { e.printStackTrace(); }
	}
	
	public void checkCapacity(int bodyLength) {
		
		if(this.peer.getCapacity() + bodyLength > this.peer.getMaximumCapacity()) {
			Enumeration<String> files = this.peer.getRecords().keys();
			boolean done = false;
			
			while(files.hasMoreElements() && !done) {
				String fileID = files.nextElement();
				
				Enumeration<String> chunks = this.peer.getRecords().get(fileID).getChunks().keys();
				
				while(chunks.hasMoreElements() && !done) {
					String chunkNo = chunks.nextElement();
					
					if(this.peer.getRecords().get(fileID).getChunks().get(chunkNo).getStoredMessages().size() > 
							this.peer.getRecords().get(fileID).getReplicationDegree()) {
						
						try {
							Files.delete(Paths.get(this.peer.getFolderPath() + "\\(" + fileID + "," + chunkNo + ")"));
						}
						catch (IOException e) { e.printStackTrace(); }
						
						this.peer.setCapacity(this.peer.getCapacity() - this.peer.getRecords().get(fileID).getChunks().get(chunkNo).getSize());
						this.peer.getRecords().get(fileID).getChunks().remove(chunkNo);
						
						sendRemovedMessage(fileID, chunkNo);
						
						if(this.peer.getCapacity() + bodyLength <= this.peer.getMaximumCapacity()) done = true;
					}
				}
			}
		}
	}
	
	public void sendRemovedMessage(String fileID, String chunkNo) {
		
		try {
			MulticastSocket MCSocket = new MulticastSocket(this.peer.getMCPort());
			
			String request = "REMOVED " + this.peer.getProtocolVersion() + " " + this.peer.getPeerID() + " "
					+ fileID + " " + chunkNo + " " + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
			
			DatagramPacket removedMessage = new DatagramPacket(request.getBytes(), request.getBytes().length, this.peer.getMCIPAddress(), this.peer.getMCPort());
			
			MCSocket.send(removedMessage);
			MCSocket.close();
		} 
		catch (IOException e) { e.printStackTrace(); }
	}
	
	public DatagramPacket buildStoredDatagramPacket(String version, String fileID, String chunkNo) {
		
		String answer = "STORED " + version + " " + this.peer.getPeerID() + " " + fileID + " " + chunkNo 
				+ (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
		
		return new DatagramPacket(answer.getBytes(), answer.getBytes().length, this.peer.getMCIPAddress(), this.peer.getMCPort());
		
	}
	
	public void storeFile(byte[] body, String fileID, String chunkNo) {
		File chunkFile = new File(this.peer.getFolderPath() + "/(" + fileID + "," + chunkNo + ")");
		
		try {
			FileOutputStream chunkFileOS = new FileOutputStream(chunkFile);
			chunkFileOS.write(body, 0, body.length);
			chunkFileOS.flush();
			chunkFileOS.close();
		} 
		catch (FileNotFoundException e) { e.printStackTrace(); } 
		catch (IOException e) { e.printStackTrace(); } 
		
	}
	
	public void checkForRemovedProcessor(String fileID, String chunkNo) {
		
		if(this.peer.getPendentProcesses().containsKey("RemovedBackup"))
			if(this.peer.getPendentProcesses().get("RemovedBackup").containsKey(fileID+chunkNo))
				((RemovedProcessor)this.peer.getPendentProcesses().get("PutChunk").get(fileID+chunkNo)).setInitiateBackupProtocol(false);
		
	}
	
	public synchronized void storeChunkInfo(String fileID, int replicationDegree, String senderID, String chunkID, int chunkSize) {
		
		if(!peer.getRecords().containsKey(fileID))
			peer.getRecords().put(fileID, new FileInfo(replicationDegree, null, senderID));
		
		if(peer.getRecords().get(fileID).addChunk(chunkID, chunkSize) != -1) 
			peer.setCapacity(peer.getCapacity() + chunkSize);
	}
}
