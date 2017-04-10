package utilities;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class FileInfo {
	
	private ConcurrentHashMap<String, ChunkInfo> chunks;
	private int replicationDegree;
	private Path filePath;
	private String serverID;
	
	public FileInfo(int replicationDegree, Path filePath, String serverID) {
		this.replicationDegree = replicationDegree;
		this.filePath = filePath;
		this.serverID = serverID;
		
		this.chunks = new ConcurrentHashMap<String, ChunkInfo>();
	}
	
	public int addChunk(String chunkID, int size) {
		if(!chunks.containsKey(chunkID)) {
			chunks.put(chunkID, new ChunkInfo(size));
			return 0;
		}
		
		return -1;
	}
	
	public void addStoredMessage(String chunkID, String storedMessage) {
		if(chunks.containsKey(chunkID)) {
			chunks.get(chunkID).addStoredMessage(storedMessage);
		}
	}
	
	public int getReplicationDegree() { return this.replicationDegree; }
	public Path getFilePath() { return this.filePath; }
	public String getServerID() { return this.serverID; }
	public ConcurrentHashMap<String, ChunkInfo> getChunks() { return chunks; }
	
	public void setReplicationDegree(int replicationDegree) { this.replicationDegree = replicationDegree; }

}
