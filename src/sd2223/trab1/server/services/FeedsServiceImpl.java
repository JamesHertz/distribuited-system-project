package sd2223.trab1.server.services;

import jakarta.ws.rs.WebApplicationException;
import static jakarta.ws.rs.core.Response.Status;

import jakarta.ws.rs.client.Client;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.FeedsService;
import sd2223.trab1.utils.RestClient;

import static sd2223.trab1.utils.Formatter.USER_FORMAT_SEP;

import java.util.List;

public class FeedsServiceImpl implements FeedsService {

    private final String domain;
   // private final Map<S>

    public FeedsServiceImpl(String domain){
        this.domain = domain;
    }
    @Override
    public long postMessage(String user, String pwd, Message msg) {
        String[] info = user.split(USER_FORMAT_SEP);

        // 	private String user;
        //	private String domain;
        //	private long creationTime;
        //	private String text;
        if(info.length != 2 || msg.getUser() == null || msg.getDomain() == null )
            throw new WebApplicationException( Status.BAD_REQUEST );

        return 0;
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) {

    }

    @Override
    public Message getMessage(String user, long mid) {
        return null;
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

}
