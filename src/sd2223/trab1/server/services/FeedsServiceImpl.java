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
        String[] info = user.split(USER_FORMAT_SEP);

        if(info.length != 2 || msg.getUser() == null || msg.getDomain() == null
            || ! this.domain.equals( msg.getDomain() ) || ! msg.getDomain().equals( info[1] ) )
        {
            Log.info("Bad request.");
            throw new WebApplicationException( Status.BAD_REQUEST );
        }

        Discovery ds = Discovery.getInstance();
        String domain = info[1];
        URI[] serverURI = ds.knownUrisOf( getServiceID(domain, USERS_SERVICE), 1);

        if(serverURI.length == 0 || gateway.getUser(serverURI[0], info[0], pwd) == null) {
            Log.info("Something is wrong with the user info :(");
            throw new WebApplicationException( Status.FORBIDDEN );
        }


        synchronized (messages){
           List<Message> userMsg = messages.computeIfAbsent(user, k -> new LinkedList<>());
           msg.setId( Message.nextID() );
           userMsg.add(msg);
        }

        return msg.getId();
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) {

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
