package sd2223.servers.java;

import sd2223.api.Message;
import sd2223.api.java.Feeds;
import sd2223.api.java.Result;

import java.util.List;
import java.util.logging.Logger;

public class JavaFeeds implements Feeds {

    private static final Logger Log = Logger.getLogger(JavaFeeds.class.getName());
    private final String domain;

    public JavaFeeds(String domain){
        this.domain = domain;
    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg) {
        Log.info("PostMessage: user=" + user + " ; pwd=" + pwd + " ; msg=" + msg);

        return null;
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED );
    }

    @Override
    public Result<Message> getMessage(String user, long mid) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED );
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED );
    }

    @Override
    public Result<Void> subscribeUser(String user, String userSub, String pwd) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED );
    }

    @Override
    public Result<Void> unSubscribeUser(String user, String userSub, String pwd) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED );
    }

    @Override
    public Result<List<String>> listSubs(String user) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED );
    }
}
