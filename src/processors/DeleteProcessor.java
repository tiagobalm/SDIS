package processors;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;

import main.Peer;

public class DeleteProcessor implements Runnable {
	
	private Peer peer;
	private Path filePath;
	
	public DeleteProcessor(Peer peer, String filename) {
		
		this.peer = peer;
		this.filePath = Paths.get(filename).toAbsolutePath();
	}

	@Override
	public void run() {
		
		String fileID = findFile();
		
		if(fileID.equals("")) {
			System.out.println("Did not find the specified file.");
			return;
		}
		
		try {
			Files.delete(filePath);
		} catch (IOException e) { e.printStackTrace(); }
		
		sendDeleteMessage(fileID);
		
	}
	
	public String findFile() {
		
		Enumeration<String> recordsKeys = peer.getRecords().keys();
		
		boolean searchComplete = false;
		String fileID = "";
		
		while(recordsKeys.hasMoreElements() && !searchComplete) {
			fileID = recordsKeys.nextElement();
			if(peer.getRecords().get(fileID).getFilePath() != null)
				if(peer.getRecords().get(fileID).getFilePath().equals(this.filePath)) searchComplete = true;
		}
		
		return searchComplete ? fileID : "";
	}

	public void sendDeleteMessage(String fileID) {
		String request = "DELETE " + this.peer.getProtocolVersion() + " " + this.peer.getPeerID() + " "
				+ fileID + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
		
		try {
			
			MulticastSocket MCSocket = new MulticastSocket(this.peer.getMCPort());
			
			DatagramPacket packet = new DatagramPacket(request.getBytes(), request.getBytes().length, this.peer.getMCIPAddress(),
					this.peer.getMCPort());
			MCSocket.send(packet);
			
			MCSocket.close();
			
		} catch (IOException e) { e.printStackTrace(); }
	}
}
