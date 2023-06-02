package sd2223.trab2.servers.rest.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import sd2223.trab2.api.Update;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.replication.ReplicatedFeedsService;
import sd2223.trab2.api.rest.FeedsService;
import sd2223.trab2.api.Message;

import java.util.List;

public class RestFeedsResource extends RestResource implements FeedsService {
    private final Feeds impl;

    public RestFeedsResource(Feeds impl){
        this.impl = impl;
    }

    @Override
    public long postMessage(Long version, String user, String pwd, Message msg) {
        return super.fromJavaResult( impl.postMessage(user, pwd, msg) );
    }

    @Override
    public void removeFromPersonalFeed(Long version, String user, long mid, String pwd) {
        super.fromJavaResult( impl.removeFromPersonalFeed(user, mid, pwd) );
    }

    @Override
    public Message getMessage(Long version, String user, long mid) {
        return super.fromJavaResult( impl.getMessage(user, mid ) );
    }


    @Override
    public List<Message> getMessages(Long version, String user, long time) {
        return super.fromJavaResult( impl.getMessages(user, time) );
    }

    @Override
    public void createFeed(Long version, String user, String secret) {
        super.fromJavaResult( impl.createFeed(user, secret) );
    }

    @Override
    public void subUser(Long version, String user, String userSub, String pwd) {
        super.fromJavaResult( impl.subscribeUser(user, userSub, pwd ) );
    }

    @Override
    public void unsubscribeUser(Long version, String user, String userSub, String pwd) {
        super.fromJavaResult( impl.unSubscribeUser(user, userSub, pwd) );
    }

    @Override
    public List<String> listSubs(Long version, String user) {
        return super.fromJavaResult( impl.listSubs( user ) );
    }

    @Override
    public List<Message> subscribeServer(Long version, String domain, String user, String secret) {
        return super.fromJavaResult( impl.subscribeServer(domain, user, secret) );
    }

    @Override
    public void createExtFeedMessage(Long version, String user, String secret, Message msg) {
        super.fromJavaResult( impl.createExtFeedMessage(user, secret, msg) );
    }

    @Override
    public void removeExtFeedMessage(Long version, String user, long mid, String secret) {
        super.fromJavaResult( impl.removeExtFeedMessage(user, mid, secret) );
    }

    @Override
    public void removeFeed(Long version, String user, String secret) {
        super.fromJavaResult( impl.removeFeed(user, secret) );
    }

    @Override
    public void removeExtFeed(Long version, String user, String secret) {
        super.fromJavaResult( impl.removeExtFeed(user, secret) );
    }

    @Override
    public void unsubscribeServer(Long version, String domain, String user, String secret) {
        super.fromJavaResult( impl.unsubscribeServer(domain, user, secret) );
    }

}
