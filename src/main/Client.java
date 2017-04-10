package main;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
	private Client() {}

    public static void main(String[] args) {
    	
    	if (args.length < 2) {
			System.out.println("Usage:java TestApp <peer_object_name> <sub_protocol> <opnd_1> <opnd_2> ");
			return;
		}
    	
    	switch(args[1]) {
    	case "BACKUP":
    		if (args.length != 4) {
    			System.out.println("Usage:java TestApp <peer_object_name> <sub_protocol> <file_path> <replication_degree> ");
    			return;
    		}
    		break;
    	case "RESTORE":
    	case "DELETE":
    		if (args.length != 3) {
    			System.out.println("Usage:java TestApp <peer_object_name> <sub_protocol> <file_path> ");
    			return;
    		}
    		break;
    	case "RECLAIM":
    		if (args.length != 3) {
    			System.out.println("Usage:java TestApp <peer_object_name> <sub_protocol> <maximum_capacity> ");
    			return;
    		}
    		break;
    	default:
    		break;
    	}
    	    	
    	rmiConnection(args);
    }
    
    public static void rmiConnection(String[] args) {
    	Registry registry;
		
		try {
			registry = LocateRegistry.getRegistry();
			Service stub = (Service) registry.lookup(args[0]);
			
			switch(args[1]) {
			case "BACKUP":
				backup(stub, args[2], Integer.parseInt(args[3]));
				break;
			case "RESTORE":
				restore(stub, args[2]);
				break;
			case "DELETE":
				delete(stub, args[2]);
				break;
			case "STATE":
				state(stub);
				break;
			case "RECLAIM":
				reclaim(stub, args[2]);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public static void backup(Service stub, String fileName, int replicationDegree) throws IOException{
		String data = "BACKUP " + fileName + " " + replicationDegree;
				
		try {            
            String response = stub.service(data);
            System.out.println("response: " + response);
        } catch (Exception e) {
        	
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
	}
	
	public static void restore(Service stub, String fileName) throws IOException{
		String data = "RESTORE " + fileName;
		
		try {            
            String response = stub.service(data);
            System.out.println("response: " + response);
        } catch (Exception e) {
        	
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
	}
	
	public static void delete(Service stub, String fileName) throws IOException{
		String data = "DELETE " + fileName;
		
		try {            
            String response = stub.service(data);
            System.out.println("response: " + response);
        } catch (Exception e) {
        	
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
	}
	
	public static void state(Service stub) throws IOException{
		String data = "STATE";
		
		try {            
            String response = stub.service(data);
            System.out.println("response: " + response);
        } catch (Exception e) {
        	
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
	}
	
	public static void reclaim(Service stub, String maximumCapacity) throws IOException {
		String data = "RECLAIM " + maximumCapacity;
		
		try {            
            String response = stub.service(data);
            System.out.println("response: " + response);
        } catch (Exception e) {
        	
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
	}
}
