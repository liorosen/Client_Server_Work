package bgu.spl.net.srv;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserInfosPro implements  Iterable<Map.Entry<String,Integer>> {
    private  ConcurrentHashMap<String, Integer> loggedInUserToIDs = new ConcurrentHashMap<>();
    private  ConcurrentHashMap<Integer,String> loggedInIDToUsers = new ConcurrentHashMap<>();

    public UserInfosPro(){

    }

    public void addUser( int clientId , String clientUserName){
        loggedInUserToIDs.put(clientUserName,clientId);
        loggedInIDToUsers.put(clientId,clientUserName);

    }

    public void deleteUser( int clientId , String clientUserName) {
        loggedInUserToIDs.remove(clientUserName,clientId);
        loggedInIDToUsers.remove(clientId,clientUserName);

    }

    public Boolean searchUser(String clientUserName){

        if(loggedInUserToIDs.get(clientUserName) != null){
                return true;
        }

        return false;
    }

    public String getUserName(int clientId){
        return loggedInIDToUsers.get(clientId);
    }

    public Integer getClientID (String userName){
        return loggedInUserToIDs.get(userName);
    }

    public void deleteUserByID( int clientId ) {
        loggedInUserToIDs.remove(clientId);
        loggedInIDToUsers.remove(clientId);

    }

    @Override
    public Iterator<Map.Entry<String, Integer>> iterator() {

        return loggedInUserToIDs.entrySet().iterator();
    }

    public boolean searchUserByID(int connectionId) {

        if(loggedInIDToUsers.get(connectionId) != null){
            return true;
        }
        return false;
    }
}
