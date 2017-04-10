package processors;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import main.Peer;
import utilities.FileInfo;

public class BackupProcessor implements Runnable {
	
	private Peer peer;
	private String filename;
	private int replicationDegree;
	private int CHUNK_SIZE = 64000;
	private Path filePath;

	public BackupProcessor(Peer peer, String filename, String replicationDegree) {
		this.peer = peer;
		this.filename = filename;
		this.replicationDegree = Integer.parseInt(replicationDegree);		
	}

	public String createFileID() throws IOException, NoSuchAlgorithmException {
		
		filePath = Paths.get(filename).toAbsolutePath();
		BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
		
		String fileMetaData = filePath + attr.lastModifiedTime().toString();
		
		java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(fileMetaData.getBytes(StandardCharsets.UTF_8));
		
		String fileID = "";
		for(int i = 0; i < hash.length; i++) {
			fileID += String.format("%02x", hash[i]);
		}
		
		return fileID;		
	}

	@Override
	public void run() {
				
		try {
			String fileID = createFileID();
			
			/* Creates a record for the file */
			if(!peer.getRecords().containsKey(fileID))
				peer.getRecords().put(fileID, new FileInfo(replicationDegree, filePath, peer.getPeerID()));
			else
				peer.getRecords().get(fileID).setReplicationDegree(replicationDegree);
			
			byte[] chunkRead = new byte[CHUNK_SIZE];
			FileInputStream reader = new FileInputStream(filename);
			
			int chunkCounter = 0;
			int n;
			
			while((n = reader.read(chunkRead)) != -1) {
				
				byte[] chunk = Arrays.copyOfRange(chunkRead, 0, n);
				
				try {
					peer.getProcessorsQueue().put(new PutChunkSender(peer, chunk, fileID, chunkCounter, replicationDegree));
				} catch (InterruptedException e) { e.printStackTrace(); }
				
				chunkCounter++;
			}
			
			reader.close();
		}
		catch (IOException e) { e.printStackTrace(); } 
		catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
	}
}
