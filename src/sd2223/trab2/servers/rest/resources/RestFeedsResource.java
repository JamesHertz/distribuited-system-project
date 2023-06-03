package sd2223.trab2.servers.rest.resources;

import org.glassfish.jersey.server.ContainerRequest;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.rest.FeedsService;
import sd2223.trab2.api.Message;

import java.util.List;

public class RestFeedsResource extends RestResource implements FeedsService {
    private final Feeds impl;

    public RestFeedsResource(Feeds impl){
        this.impl = impl;
    }

    @Override
    public long postMessage(ContainerRequest request, String user, String pwd, Message msg) {
        return super.fromJavaResult( impl.postMessage(user, pwd, msg) );
    }

    @Override
    public void removeFromPersonalFeed(ContainerRequest request, String user, long mid, String pwd) {
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
    public void createFeed(ContainerRequest request, String user, String secret) {
        super.fromJavaResult( impl.createFeed(user, secret) );
    }

    @Override
    public void subUser(ContainerRequest request, String user, String userSub, String pwd) {
        super.fromJavaResult( impl.subscribeUser(user, userSub, pwd ) );
    }

    @Override
    public void unsubscribeUser(ContainerRequest request, String user, String userSub, String pwd) {
        super.fromJavaResult( impl.unSubscribeUser(user, userSub, pwd) );
    }

    @Override
    public List<String> listSubs(Long version, String user) {
        return super.fromJavaResult( impl.listSubs( user ) );
    }

    @Override
    public List<Message> subscribeServer(ContainerRequest request, String domain, String user, String secret) {
        return super.fromJavaResult( impl.subscribeServer(domain, user, secret) );
    }

    @Override
    public void createExtFeedMessage(ContainerRequest request, String user, String secret, Message msg) {
        super.fromJavaResult( impl.createExtFeedMessage(user, secret, msg) );
    }

    @Override
    public void removeExtFeedMessage(ContainerRequest request, String user, long mid, String secret) {
        super.fromJavaResult( impl.removeExtFeedMessage(user, mid, secret) );
    }

    @Override
    public void removeFeed(ContainerRequest request, String user, String secret) {
        super.fromJavaResult( impl.removeFeed(user, secret) );
    }

    @Override
    public void removeExtFeed(ContainerRequest request, String user, String secret) {
        super.fromJavaResult( impl.removeExtFeed(user, secret) );
    }

    @Override
    public void unsubscribeServer(ContainerRequest request, String domain, String user, String secret) {
        super.fromJavaResult( impl.unsubscribeServer(domain, user, secret) );
    }

}
