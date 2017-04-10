package processors;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import main.Peer;

public class SpaceReclaimProcessor implements Runnable {
	private Peer peer;
	private int numberOfBytes;
	
	public SpaceReclaimProcessor(Peer peer, String numberOfBytes) {
		this.peer = peer;
		this.numberOfBytes = Integer.parseInt(numberOfBytes);
	}

	@Override
	public void run() {
				
		this.peer.setMaximumCapacity(this.numberOfBytes);
		
		manageSpace();
	}
	
	public void manageSpace() {
				
		if(this.peer.getCapacity() > this.peer.getMaximumCapacity()) {
			
			List<File> filesStored = getStoredFiles();
			
			Collections.sort(filesStored, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					return Long.valueOf(o1.lastModified()).compareTo(o2.lastModified());
				}           
			});
			
			deleteFiles(filesStored);
		}		
	}
	
	public void deleteFiles(List<File> filesStored) {
				
		try {
			MulticastSocket MCSocket = new MulticastSocket(this.peer.getMCPort());
			
			while(this.peer.getCapacity() > this.peer.getMaximumCapacity()) {
				File file = filesStored.get(0);
				
				String filename = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("\\") + 1);
				String fileID = filename.substring(1, filename.indexOf(","));
				String chunkNo = filename.substring(filename.indexOf(",") + 1, filename.lastIndexOf(")"));
				
				System.out.println("FileID: " + fileID + " ChunkNo: " + chunkNo);
				
				try {
					Files.delete(file.toPath());
				} catch (IOException e) { e.printStackTrace(); }
								
				updateCapacity(fileID, chunkNo);		
				sendRemovedMessage(MCSocket, fileID, chunkNo);
				filesStored.remove(0);
				
				System.out.println("Capacity: " + this.peer.getCapacity());
			}
			
			MCSocket.close();
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public void updateCapacity(String fileID, String chunkNo) {
		
		this.peer.setCapacity(this.peer.getCapacity() - this.peer.getRecords().get(fileID).getChunks().get(chunkNo).getSize());
		this.peer.getRecords().get(fileID).getChunks().remove(chunkNo);
	}
	
	public void sendRemovedMessage(MulticastSocket MCSocket, String fileID, String chunkNo) {
		String request = "REMOVED " + this.peer.getProtocolVersion() + " " + this.peer.getPeerID() + " "
				+ fileID + " " + chunkNo + " " + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
		
		DatagramPacket removedMessage = new DatagramPacket(request.getBytes(), request.getBytes().length, this.peer.getMCIPAddress(), this.peer.getMCPort());
		
		try {
			MCSocket.send(removedMessage);
		} catch (IOException e) { e.printStackTrace(); }
		
	}
	
	public ArrayList<File> getStoredFiles() {		
		File[] files = this.peer.getFolderPath().toFile().listFiles();
		
		return new ArrayList<File>(Arrays.asList(files));
	}
}
