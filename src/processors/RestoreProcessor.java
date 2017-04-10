package processors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import main.Peer;

public class RestoreProcessor implements Runnable {
	private Peer peer;
	private Path filePath;
	private ConcurrentHashMap<Integer, byte[]> chunks;
	private MulticastSocket MCSocket;
	
	public RestoreProcessor(Peer peer, String filename) {
		
		this.peer = peer;
		filePath = Paths.get(filename).toAbsolutePath();
		this.chunks = new ConcurrentHashMap<Integer, byte[]>();
		
		if(!peer.getPendentProcesses().containsKey("Restore"))
			peer.getPendentProcesses().put("Restore", new ConcurrentHashMap<String, Runnable>());
		
		try {
			MCSocket = new MulticastSocket(peer.getMCPort());
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public String findFile() {
		
		Enumeration<String> recordsKeys = peer.getRecords().keys();
		
		boolean searchComplete = false;
		String fileID = "";
		
		while(recordsKeys.hasMoreElements() && !searchComplete) {
			fileID = recordsKeys.nextElement();
			if(peer.getRecords().get(fileID).getFilePath() != null)
				if(peer.getRecords().get(fileID).getFilePath().equals(filePath)) searchComplete = true;
		}
		
		return fileID;
	}

	@Override
	public void run() {
		
		String fileID = findFile();
		
		if(fileID.equals("")) {
			System.out.println("Not able to restore file.");
			return;
		}
				
		if(!peer.getPendentProcesses().get("Restore").containsKey(fileID))
			peer.getPendentProcesses().get("Restore").put(fileID, this);
				
		Enumeration<String> chunksSet = peer.getRecords().get(fileID).getChunks().keys();
				
		while(chunksSet.hasMoreElements())
			sendGetChunkMessage(chunksSet.nextElement(), fileID);
				
		restoreFile(fileID);
		
		peer.getPendentProcesses().get("Restore").remove(fileID);
	}
	
	public void sendGetChunkMessage(String chunkNo, String fileID) {
		String header = "GETCHUNK " + peer.getProtocolVersion() + " " + peer.getPeerID() + " " + fileID + " " + chunkNo +
				 (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
		
		System.out.println(header.trim());
		
		DatagramPacket packet = new DatagramPacket(header.getBytes(), header.getBytes().length, peer.getMCIPAddress(), peer.getMCPort());
    	
		try {
			MCSocket.send(packet);
		} catch (IOException e) { e.printStackTrace(); }		
	}
	
	public boolean checkChunks(String fileID) {
		int numberOfTries = 0;
		boolean restoreComplete = false;
		
		do {
			
			if(peer.getRecords().get(fileID).getChunks().size() == chunks.size())
				restoreComplete = true;
			
			numberOfTries++;
			
			try {
				Thread.sleep((long)1000);
			} 
			catch (InterruptedException e) { e.printStackTrace(); }
			
		} while(numberOfTries < 5 && !restoreComplete);
		
		return restoreComplete;
	}
	
	public void restoreFile(String fileID) {
		boolean restoreComplete = checkChunks(fileID);
				
		if(!restoreComplete) {
			System.out.println("It wasn't possible to restore the file.");
			return;
		}
				
		try {
		
			File restoredFile = new File(filePath.toString());
			
			if(restoredFile.exists())
				new PrintWriter(restoredFile).close();
			else
				restoredFile.createNewFile();
			
			FileOutputStream restoredFileStream = new FileOutputStream(restoredFile);
			
			Enumeration<Integer> chunkSet = chunks.keys();
			List<Integer> list = Collections.list(chunkSet);
	        Collections.sort(list);
	        
	        System.out.println("Restoring...");
	        
	        for (Integer chunkNo : list) {		
	        	System.out.println("Chunk #" + chunkNo + " ChunkSize: " + chunks.get(chunkNo).length);
		    	restoredFileStream.write(chunks.get(chunkNo));
		    	restoredFileStream.flush();
	        }
	        
	        restoredFileStream.close();
		}
		catch (FileNotFoundException e) { e.printStackTrace();	} 
		catch (IOException e) {	e.printStackTrace(); }
		
	}
	
	public ConcurrentHashMap<Integer, byte[]> getChunks() { return chunks; }
	
	public void addChunk(Integer chunkNo, byte[] body) {
		if(!this.chunks.contains(chunkNo))
			this.chunks.put(chunkNo, body);
	}

}
