package main;
import java.rmi.registry.Registry;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import listeners.*;
import parser.StoreServerStateData;
import parser.readServerStateData;
import processors.*;
import utilities.FileInfo;
import workers.Worker;

public class Peer implements Service {
	
	private String protocolVersion, remoteObjectName;
	private static String serverID;
	private InetAddress MCIPAddress, MDBIPAddress, MDRIPAddress;
	private int MCPort;
	private int MDBPort;
	private int MDRPort;
	private static BlockingQueue<Runnable> queue;
	private static ScheduledExecutorService executor;
	private static int numberOfWorkerThreads = 20;
	private static Path folderPath;
	private static ConcurrentHashMap<String, FileInfo> records;
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, Runnable>> pendentProcesses;
	private static long maximumCapacity = 6400000;
	private static long capacity = 0;
	
	public Peer(String protocolVersion, String serverID , String remoteObjectName, String MCIPAddress, String MCPort, 
    		String MDBIPAddress, String MDBPort, String MDRIPAddress, String MDRPort) {
		
		
		this.protocolVersion = protocolVersion;
		Peer.serverID = serverID;
		this.remoteObjectName = remoteObjectName;
		
		try {
			this.MCIPAddress = InetAddress.getByName(MCIPAddress);
			this.MDBIPAddress =  InetAddress.getByName(MDBIPAddress);;
			this.MDRIPAddress =  InetAddress.getByName(MDRIPAddress);;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		this.MCPort = Integer.parseInt(MCPort);
		this.MDBPort = Integer.parseInt(MDBPort);
		this.MDRPort = Integer.parseInt(MDRPort);
		
		queue = new ArrayBlockingQueue<Runnable>(500);
		executor = Executors.newScheduledThreadPool(numberOfWorkerThreads);
		records = new ConcurrentHashMap<String, FileInfo>();
		pendentProcesses = new ConcurrentHashMap<String, ConcurrentHashMap<String, Runnable>>();
	}
	
	public String backup(String filename, String replicationDegree) {
		
		queue.add(new BackupProcessor(this, filename, replicationDegree));
		return "Backup complete!";
	}
	
	public String restore(String filename) {
		queue.add(new RestoreProcessor(this, filename));
		return "Restoring file...";
	}
	
	public String delete(String filename) {
		queue.add(new DeleteProcessor(this, filename));
		return "File deleted!";
	}
	
	public String reclaim(String numberOfBytes) {
		queue.add(new SpaceReclaimProcessor(this, numberOfBytes));
		return "Space reclaimed!";
	}
	
	public String state() {
		String data = "";
		
	    try {
			data = new String(Files.readAllBytes(Paths.get(folderPath.toString() + "\\stateData.txt")));
		} catch (IOException e) { e.printStackTrace(); }
	    
	    return data;
	}
		
    @Override
    public String service(String request) {
    	
    	String[] splitRequest = request.split(" +");
    	    	
    	switch(splitRequest[0]) {
    	case "BACKUP":
    		return backup(splitRequest[1], splitRequest[2]);
    	case "RESTORE":
    		return restore(splitRequest[1]);
    	case "DELETE":
    		return delete(splitRequest[1]);
    	case "RECLAIM":
    		return reclaim(splitRequest[1]);
    	case "STATE":
    		return state();
    	default:
    		return "Invalid Operation.";    		
    	} 	
    }
    
    public static void createStub(Peer peer) {
    	
		try {
			Service stub = (Service) UnicastRemoteObject.exportObject(peer, 0);
			Registry registry = LocateRegistry.getRegistry();
	        registry.rebind(peer.remoteObjectName, stub);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    }
    
    public static void startListeners(Peer peer) {
    	
    	MCListener MCListener = new MCListener(peer);
        MDBListener MDBListener = new MDBListener(peer);
        MDRListener MDRListener = new MDRListener(peer);
        
        MCListener.start();
        MDBListener.start();
        MDRListener.start();
    }
    
    public static void startWorkers(BlockingQueue<Runnable> queue) {
    	
    	for(int i = 0; i < (numberOfWorkerThreads / 2); i++) {
    		executor.submit(new Worker(queue));
    	}
    }
    
    public static void createDirectory() throws IOException {
    	
    	folderPath = Paths.get("Peer"+serverID).toAbsolutePath();
        Files.createDirectories(folderPath);
        
    }
    
    public static void main(String args[]) {
    	
    	if (args.length != 9) {
			System.out.println("Usage:java Peer <protocol_version> <server_id> <remote_object_name>"
					+ " <mc_ip_address> <mc_port_number> <mdb_ip_address> <mdb_port_number> <mdr_ip_address> "
					+ "<mdr_port_number>");
			return;
		}
    	
        try {
            Peer peer = new Peer(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
            
            createDirectory();
            createStub(peer);
            startListeners(peer);
            startWorkers(queue);
            
            new readServerStateData(peer).run();
            
            executor.scheduleAtFixedRate(new StoreServerStateData(peer), 1, 1, TimeUnit.SECONDS);

            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
    public String getPeerID() { return serverID; }
    public String getProtocolVersion() { return this.protocolVersion; }
    public String getRemoteObjectName() { return this.remoteObjectName; }
    public InetAddress getMCIPAddress() { return this.MCIPAddress; }
    public InetAddress getMDBIPAddress() { return this.MDBIPAddress; }
    public InetAddress getMDRIPAddress() { return this.MDRIPAddress; }
    public int getMCPort() { return this.MCPort; }
    public int getMDBPort() { return this.MDBPort; }
    public int getMDRPort() { return this.MDRPort; }
    public BlockingQueue<Runnable> getProcessorsQueue() { return queue; }
    public ScheduledExecutorService getExecutor() { return executor; }
    public int getNumberOfWorkerThreads() { return numberOfWorkerThreads; }
    public Path getFolderPath() { return folderPath; }
    public ConcurrentHashMap<String, FileInfo> getRecords() { return records; }
    public long getMaximumCapacity() { return maximumCapacity; }
    public long getCapacity() { return capacity; }
    public ConcurrentHashMap<String, ConcurrentHashMap<String, Runnable>> getPendentProcesses() { return pendentProcesses; }
    
    public void setCapacity (long newCapacity) { capacity = newCapacity;}
    public void setMaximumCapacity (long newMaximumCapacity) { maximumCapacity = newMaximumCapacity; }
    
}
