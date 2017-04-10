package processors;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.Peer;

public class DeleteReceiverProcessor implements Runnable {
	private Peer peer;
	private DatagramPacket packet;
	private String[] headerAttributes;
	
	public DeleteReceiverProcessor(Peer peer, DatagramPacket packet){
		this.peer = peer;
		this.packet = packet;
	}
	
	@Override
	public void run() {
		
		processRequest();
		
		String senderID = this.headerAttributes[2];
		
		if(!this.peer.getPeerID().equals(senderID)) {
		
			String fileID = this.headerAttributes[3];
			File[] chunks = this.peer.getFolderPath().toFile().listFiles(new FilenameFilter() {
			    @Override
			    public boolean accept(File dir, String name) {
			    	
			    	Pattern p = Pattern.compile("("+fileID+",\\d*)");
			    	Matcher m = p.matcher(name);
			    	boolean b = m.find();
			    				    	
			        return  b;
			    }
			});
			
			try {
				for (File chunk : chunks) {
					this.peer.setCapacity(this.peer.getCapacity() - chunk.length());
					Files.delete(Paths.get(chunk.getAbsolutePath()));
				}
			} catch (IOException e) { e.printStackTrace(); }
			
			this.peer.getRecords().remove(fileID);
		}
	}
	
	public void processRequest() {
		String endHeader = "" + (char)0x0D + (char)0x0A + (char)0x0D + (char)0x0A;
		String header = new String(Arrays.copyOfRange(packet.getData(), 0, new String(packet.getData()).indexOf(endHeader)));
		
		System.out.println(header);
		
		this.headerAttributes = header.split(" +");
	}
}
