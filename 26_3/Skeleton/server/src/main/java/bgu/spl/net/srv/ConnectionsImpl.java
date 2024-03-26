package bgu.spl.net.srv;
import java.io.IOException;
import java.util.HashMap;

public class ConnectionsImpl implements  Connections<byte[]>{
        HashMap<Integer , ConnectionHandler<byte[]>> connections = new HashMap<>();


        public void connect(int connectionId, ConnectionHandler<byte[]> handler) {
            connections.put(connectionId, handler);
        }

        public boolean send(int connectionId, byte[] msg) {
             if(connections.containsKey(connectionId)){
                 connections.get(connectionId).send(msg);
                 return true;
            }
             return false;
        }

        public void disconnect(int connectionId) {
            connections.remove(connectionId);
        }

        //TODO: BIG ADDITION CHECK FREQUENTLY THIS AND IT'S INTERFACE
        public ConnectionHandler<byte[]> getHandler(int connectionId) {
            return connections.get(connectionId);
        }
}
