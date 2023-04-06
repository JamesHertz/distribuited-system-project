package sd2223.api.java;

import sd2223.api.Message;

import java.util.List;

public interface Feeds {

    Result<Long> postMessage(String user, String pwd, Message msg);

    Result<Void> removeFromPersonalFeed(String user, long mid, String pwd);

    Result<Message> getMessage(String user, long mid);

    Result<List<Message>> getMessages(String user, long time);

    Result<Void> subscribeUser(String user, String userSub, String pwd);

    Result<Void> unSubscribeUser(String user, String userSub, String pwd);

    Result<List<String>> listSubs(String user);
}
