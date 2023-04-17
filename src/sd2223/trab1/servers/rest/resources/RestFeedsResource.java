package sd2223.trab1.servers.rest.resources;

import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.rest.FeedsService;
import sd2223.trab1.api.Message;

import java.util.List;

public class RestFeedsResource extends RestResource implements FeedsService {
    private final Feeds impl;

    public RestFeedsResource(Feeds impl){
        this.impl = impl;
    }

    @Override
    public long postMessage(String user, String pwd, Message msg) {
        return super.fromJavaResult( impl.postMessage(user, pwd, msg) );
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) {
        super.fromJavaResult( impl.removeFromPersonalFeed(user, mid, pwd) );
    }

    @Override
    public Message getMessage(String user, long mid) {
        return super.fromJavaResult( impl.getMessage(user, mid ) );
    }


    @Override
    public List<Message> getMessages(String user, long time) {
        return super.fromJavaResult( impl.getMessages(user, time) );
    }

    @Override
    public void createFeed(String user) {
        super.fromJavaResult( impl.createFeed(user) );
    }

    @Override
    public void subUser(String user, String userSub, String pwd) {
        super.fromJavaResult( impl.subscribeUser(user, userSub, pwd ) );
    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd) {
        super.fromJavaResult( impl.unSubscribeUser(user, userSub, pwd) );
    }

    @Override
    public List<String> listSubs(String user) {
        return super.fromJavaResult( impl.listSubs( user ) );
    }

    @Override
    public void subscribeServer(String domain, String user) {
        super.fromJavaResult( impl.subscribeServer(domain, user) );
    }

    @Override
    public void createExtFeedMessage(String user, Message msg) {
        super.fromJavaResult( impl.createExtFeedMessage(user, msg) );
    }

    @Override
    public void removeExtFeedMessage(String user, long mid) {
        super.fromJavaResult( impl.removeExtFeedMessage(user, mid) );
    }

    @Override
    public void removeFeed(String user) {
        super.fromJavaResult( impl.removeFeed(user) );
    }

    @Override
    public void removeExtFeed(String user) {
        super.fromJavaResult( impl.removeExtFeed(user) );
    }


}
