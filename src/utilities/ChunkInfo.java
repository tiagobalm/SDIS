package utilities;

import java.util.Vector;

public class ChunkInfo {
	private int size;
	private Vector<String> storedMessages;
	
	public ChunkInfo(int size) {
		this.size = size;
		storedMessages = new Vector<String>();
	}
	
	public void addStoredMessage(String storedMessage) {
		if(!storedMessages.contains(storedMessage))
			storedMessages.add(storedMessage);
	}
	
	public int getSize() { return size; }
	public Vector<String> getStoredMessages() { return storedMessages; }

}
