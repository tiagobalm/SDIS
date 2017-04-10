package parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import main.Peer;
import utilities.FileInfo;

public class readServerStateData implements Runnable {

	private Peer peer;
	private int state;
	
	public readServerStateData(Peer peer) {
		this.peer = peer;
		this.state = 0;
	}
	
	@Override
	public void run() {
				
		try {
			
			BufferedReader br = new BufferedReader(new FileReader(this.peer.getFolderPath().toString() + "\\stateData.txt"));

			String line = br.readLine();
			
			String fileID = "", filePath = "", backupServer = "", desiredReplicationDegree = "", chunkNo = "", chunkSize = "",
					storedMessage = "";
			
		    while (line != null) {
		    	
		    	updateState(line);

		    	switch(state) {
		    	case 0:
		    		fileID = line.substring(line.indexOf(":") + 2, line.length());
		    		break;
		    	case 1:
		    		filePath = line.substring(line.indexOf(":") + 2, line.length());
		    		break;
		    	case 2:
		    		backupServer = line.substring(line.indexOf(":") + 2, line.length());
		    		break;
		    	case 3:
		    		desiredReplicationDegree = line.substring(line.indexOf(":") + 2, line.length());
		    		createFileInfo(fileID, filePath, backupServer, desiredReplicationDegree);
		    		break;
		    	case 4:
		    		chunkNo = line.substring(line.indexOf("#") + 1, line.length());
		    		break;
		    	case 5:
		    		chunkSize = line.substring(line.indexOf(":") + 2, line.length());
		    		createChunkInfo(fileID, chunkNo, chunkSize);
		    		break;
		    	case 6:		    		
		    		break;
		    	case 7:
		    		storedMessage = line.substring(line.indexOf(":") + 2, line.length());
		    		storeMessage(fileID, chunkNo, storedMessage);
		    		break;
		    	}
		    	
		    	line = br.readLine();
		    }
		    br.close();
		    
		} 
		catch (IOException e) { }
	}
	
	public void createFileInfo(String fileID, String filePath, String backupServer, String desiredReplicationDegree) {
		
		if(filePath.equals("") || filePath == null)
			this.peer.getRecords().put(fileID, new FileInfo(Integer.parseInt(desiredReplicationDegree), null, backupServer));
		else
			this.peer.getRecords().put(fileID, new FileInfo(Integer.parseInt(desiredReplicationDegree), Paths.get(filePath).toAbsolutePath(), backupServer));
	}
	
	public void createChunkInfo(String fileID, String chunkNo, String chunkSize) {
		this.peer.getRecords().get(fileID).addChunk(chunkNo, Integer.parseInt(chunkSize));
	}
	
	public void storeMessage(String fileID, String chunkNo, String storedMessage) {
		this.peer.getRecords().get(fileID).getChunks().get(chunkNo).getStoredMessages().add(storedMessage);
	}
	
	public void updateState(String line) {
		
		if(line.contains("File ID:")) state = 0;
    	else if(line.contains("FilePath:")) state = 1;
    	else if(line.contains("Server who initiated the backup:")) state = 2;
    	else if(line.contains("Desired Replication Degree:")) state = 3;
    	else if(line.contains("Chunk #")) state = 4;
    	else if(line.contains("Chunk Size:")) state = 5;
    	else if(line.contains("Perceived Replication Degree:")) state = 6;
    	else state = 7;
		
	}

}
