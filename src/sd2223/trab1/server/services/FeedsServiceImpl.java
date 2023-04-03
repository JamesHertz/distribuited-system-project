package sd2223.trab1.server.services;

import jakarta.ws.rs.WebApplicationException;
import static jakarta.ws.rs.core.Response.Status;

import jakarta.ws.rs.client.Client;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.FeedsService;
import sd2223.trab1.discovery.Discovery;
import sd2223.trab1.utils.Formatter;
import sd2223.trab1.utils.RestClient;

import static sd2223.trab1.utils.Formatter.*;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class FeedsServiceImpl implements FeedsService {


    private static Logger Log = Logger.getLogger(FeedsServiceImpl.class.getName());
    private final String domain;
    private final RestClient gateway;
    private final Map<String, List<Message>> messages;
   // private final Map<S>

    public FeedsServiceImpl(String domain){
        this.domain = domain;
        this.gateway = new RestClient();
        this.messages = new HashMap<>();
    }

    @Override
    public long postMessage(String user, String pwd, Message msg) {
        Log.info("PostMessage: user=" + user + " ; pwd=" + pwd + " ; msg=" + msg);
        String[] info = user.split(USER_FORMAT_SEP); // name, domain

        if(     info.length != 2   //Bad format
                || msg.getUser() == null  //No message
                || msg.getDomain() == null  //No domain
                || ! this.domain.equals( msg.getDomain()) //Trying to send message to other domain that is not this one
                || ! msg.getDomain().equals( info[1] ) ) //Trying to send message to other domain that is not the users domain
        {
            Log.info("Bad request.");
            throw new WebApplicationException( Status.BAD_REQUEST );
        }

        Discovery ds = Discovery.getInstance(); //Let's get discovery
        String domain = info[1];
        URI[] serverURI = ds.knownUrisOf( getServiceID(domain, USERS_SERVICE), 1); //And check if this server domain is active here

        if(     serverURI.length == 0 //No server domains found
                || gateway.getUser(serverURI[0], info[0], pwd) == null) //If password incorrect and if user exists
        {
            Log.info("Something is wrong with the user info :(");
            throw new WebApplicationException( Status.FORBIDDEN );
        }


        synchronized (messages){ //Update the messages of the user :)
           List<Message> userMsg = messages.computeIfAbsent(user, k -> new LinkedList<>());
           msg.setId( Message.nextID() );
           userMsg.add(msg);
        }

        return msg.getId();
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) {
        String[] info = user.split(USER_FORMAT_SEP); // name, domain

        Discovery ds = Discovery.getInstance(); //Let's get discovery
        String domain = info[1];
        URI[] serverURI = ds.knownUrisOf( getServiceID(domain, USERS_SERVICE), 1); //And check if this server domain is active here

        if(     serverURI.length == 0 //No server domains found
                || gateway.getUser(serverURI[0], info[0], pwd) == null) //If password incorrect (?)
        {
            Log.info("Something is wrong with the user info :(");
            throw new WebApplicationException( Status.FORBIDDEN );
        }

        synchronized (messages){ //Update the messages of the user :)
            List<Message> userMsg = messages.get(user);
            if(userMsg == null){
                Log.info("User has no messages");
                throw new WebApplicationException( Status.NOT_FOUND);
            }
            int targetIdx = 0;
            for (Message m : userMsg) {
                if(m.getId() == mid){
                    break;
                }
                targetIdx++;
            }
            if(targetIdx == userMsg.size()){
                Log.info("Message not found");
                throw new WebApplicationException( Status.NOT_FOUND);
            }

            userMsg.remove(targetIdx);
        }

    }

    @Override
    public Message getMessage(String user, long mid) {
        Log.info("getMessage: user=" + user + " ; " + " mid=" + mid);
        if(user == null){
            Log.info("Invalid request.");
            throw new WebApplicationException( Status.BAD_REQUEST );
        }

        Message msg;
        synchronized (messages){
            msg = this.findMessage(user, mid);
        }
        if(msg == null) {
            Log.info("Message or user doesn't exist.");
            throw new WebApplicationException( Status.NOT_FOUND );
        }
        return msg;
    }

    @Override
    public List<Message> getMessages(String user, long time) {
        return null;
    }

    @Override
    public void subUser(String user, String userSub, String pwd) {

    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd) {

    }

    @Override
    public List<String> listSubs(String user) {
        return null;
    }
    private Message findMessage(String user, long mid){
        var msgs = messages.get(user);
        if(msgs == null) return null;
        for(var m : msgs) if(m.getId() == mid) return m;
        return null;
    }

}
