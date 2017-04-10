package processors;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import main.Peer;

public class GetChunkProcessor implements Runnable {
	
	private Peer peer;
	private DatagramPacket packet;
	private int CHUNK_SIZE = 64000;
	private MulticastSocket MDRSocket;
	private String[] headerAttributes;
	private boolean send;
	
	public GetChunkProcessor(Peer peer, DatagramPacket packet) {
		this.peer = peer;
		this.packet = packet;
		this.send = true;
		
		try {
			this.MDRSocket = new MulticastSocket(this.peer.getMDRPort());
		} catch (IOException e) { e.printStackTrace(); }
		
		
		if(!this.peer.getPendentProcesses().containsKey("Restore"))
			this.peer.getPendentProcesses().put("Restore", new ConcurrentHashMap<String, Runnable>());
	}

	@Override
	public void run() {
		
		String endHeader = "" + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
		String header = new String(Arrays.copyOfRange(packet.getData(), 0, new String(packet.getData()).indexOf(endHeader)));
		this.headerAttributes = header.split(" +");
		
		String senderID = this.headerAttributes[2];
		String fileID = this.headerAttributes[3];
		String chunkNo = this.headerAttributes[4];
		
		/*Check if this is not the initiatorpeer */
		if(!senderID.equals(this.peer.getPeerID())) {
					
			String filePath = getFilePath();
			
			try {
				
				ByteArrayOutputStream data = createDataPacket(filePath);
				
				this.peer.getPendentProcesses().get("Restore").put(fileID+chunkNo, this);
				
				long randomWait = new Random().nextInt(401);
				Thread.sleep(randomWait);
				
				if(send) {
					System.out.println("Sending chunk #" + chunkNo +"...");
					DatagramPacket packetToSend = new DatagramPacket(data.toByteArray(), data.toByteArray().length, this.peer.getMDRIPAddress(),
							this.peer.getMDRPort());
					System.out.println("Getchunk: Packet size: " + packetToSend.getData().length);
					this.MDRSocket.send(packetToSend);
				}
				else
					System.out.println("Request already answered. Not sending chunk #" + chunkNo + ".");
				
				MDRSocket.close();
				
				this.peer.getPendentProcesses().get("Restore").remove(fileID+chunkNo);
			
			} catch (IOException | InterruptedException e) { e.printStackTrace(); }
		}
	}
	
	public ByteArrayOutputStream createDataPacket(String filePath) throws IOException {
		
		FileInputStream chunkReader = new FileInputStream(filePath);
		
		byte[] chunkRead = new byte[CHUNK_SIZE];
		int bytesRead = chunkReader.read(chunkRead);
		chunkReader.close();
		
		byte[] chunk = Arrays.copyOfRange(chunkRead, 0, bytesRead);
		
		String headerChunk = "CHUNK " + this.headerAttributes[1] + " " + this.peer.getPeerID() + " " + this.headerAttributes[3] 
				+ " " + this.headerAttributes[4] + " " + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
		
		ByteArrayOutputStream data = new ByteArrayOutputStream( );
		data.write(headerChunk.getBytes());
		data.write(chunk);
		
		return data;
	}
	
	public String getFilePath() {
		
		String fileID = this.headerAttributes[3];
		String chunkNo = this.headerAttributes[4];
		
		return this.peer.getFolderPath() + "\\(" + fileID + "," + chunkNo + ")";
	}
	
	public void setSend(boolean send) { this.send = send; }
}
