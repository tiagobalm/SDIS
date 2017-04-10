package main;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Service extends Remote {
    String service(String request) throws RemoteException;
}
