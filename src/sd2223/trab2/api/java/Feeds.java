package sd2223.trab2.api.java;

import sd2223.trab2.api.Message;

import java.util.List;

public interface Feeds {

    Result<Long> postMessage(String user, String pwd, Message msg);

    Result<Void> removeFromPersonalFeed(String user, long mid, String pwd);

    Result<Message> getMessage(String user, long mid);

    Result<List<Message>> getMessages(String user, long time);

    Result<Void> subscribeUser(String user, String userSub, String pwd);

    Result<Void> unSubscribeUser(String user, String userSub, String pwd);

    Result<List<String>> listSubs(String user);

    Result<List<Message>> subscribeServer(String domain, String user, String secret);

    Result<Void> createFeed(String user, String secret);

    Result<Void> createExtFeedMessage(String user, String secret, Message msg);

    Result<Void> removeExtFeedMessage(String user , long mid, String secret);

    Result<Void> removeFeed ( String user, String secret);
    Result<Void> removeExtFeed ( String user, String secret);

    Result<Void> unsubscribeServer(String domain, String user, String secret);
}
