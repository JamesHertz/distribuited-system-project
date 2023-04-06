package sd2223.servers.rest.resources;

import sd2223.api.java.Feeds;
import sd2223.api.rest.FeedsService;
import sd2223.servers.java.JavaFeeds;
import sd2223.api.Message;

import java.util.List;

public class RestFeedsResource extends RestResource implements FeedsService {
    private final Feeds impl;
    public RestFeedsResource(String domain){
        impl = new JavaFeeds(domain);
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
}