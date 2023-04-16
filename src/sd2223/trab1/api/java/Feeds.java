package sd2223.trab1.api.java;

import sd2223.trab1.api.Message;

import java.util.List;

public interface Feeds {

    Result<Long> postMessage(String user, String pwd, Message msg);

    Result<Void> removeFromPersonalFeed(String user, long mid, String pwd);

    Result<Message> getMessage(String user, long mid);

    Result<List<Message>> getMessages(String user, long time);

    Result<Void> subscribeUser(String user, String userSub, String pwd);

    Result<Void> unSubscribeUser(String user, String userSub, String pwd);

    Result<List<String>> listSubs(String user);

    Result<Void> subscribeServer(String domain, String user);

    Result<Void> createFeed(String user);

    Result<Void> receiveMessage(String user, Message msg );

    // Result<Void> unsubscribeServer(String domain, String user);
    // Result<Void> removeFeedMessage ( String user , long mid );
    // Result<Void> removeFeed ( String user ); // think about this
}
