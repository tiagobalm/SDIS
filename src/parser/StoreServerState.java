package parser;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import main.Peer;

public class StoreServerState implements Runnable {
	
	private Peer peer;
	
	public StoreServerState(Peer peer) {
		this.peer = peer;
	}

	@Override
	public void run() {
		
		try{
		    PrintWriter writer = new PrintWriter(this.peer.getFolderPath().toString() + "\\stateData.txt", "UTF-8");
		    
		    Enumeration<String> files = this.peer.getRecords().keys();
		    
		    while(files.hasMoreElements()) {
		    	String fileID = files.nextElement();
		    	
		    	writer.println("File ID: " + fileID);
		    	
		    	if(this.peer.getRecords().get(fileID).getFilePath() != null)
		    		writer.println("  FilePath: " + this.peer.getRecords().get(fileID).getFilePath().toString());
		    	writer.println("  Server who initiated the backup: " + this.peer.getRecords().get(fileID).getServerID());
		    	writer.println("  Desired Replication Degree: " + this.peer.getRecords().get(fileID).getReplicationDegree());
		    	
		    	Enumeration<String> chunks = this.peer.getRecords().get(fileID).getChunks().keys();
		    	
		    	while(chunks.hasMoreElements()) {
		    		String chunkNo = chunks.nextElement();
		    		
		    		writer.println("    Chunk #" + chunkNo);
		    		writer.println("      Chunk Size: " + this.peer.getRecords().get(fileID).getChunks().get(chunkNo).getSize());
		    		writer.println("      Perceived Replication Degree: " + this.peer.getRecords().get(fileID).getChunks().get(chunkNo).getStoredMessages().size());
		    		
		    		for(int i = 0; i < this.peer.getRecords().get(fileID).getChunks().get(chunkNo).getStoredMessages().size(); i++)
		    			writer.println("        Stored Message: " + this.peer.getRecords().get(fileID).getChunks().get(chunkNo).getStoredMessages().get(i));
		    	
		    	}
		    }
		    
		    writer.close();
		} 
		catch (IOException e) { e.printStackTrace(); }
		
	}

}
