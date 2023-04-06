package sd2223.servers.java;

import sd2223.api.Message;
import sd2223.api.java.Feeds;
import sd2223.api.java.Result;
import sd2223.api.java.Users;
import sd2223.clients.UsersClientFactory;
import sd2223.trab1.discovery.Discovery;
import sd2223.utils.Formatter;
import sd2223.utils.IDGenerator;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import static sd2223.api.java.Result.ErrorCode;
public class JavaFeeds implements Feeds {

    private static final Logger Log = Logger.getLogger(JavaFeeds.class.getName());

    private final String domain;
    private final IDGenerator generator;
    private final Map<String, Map<Long, Message>> messages;

    public JavaFeeds(String domain, long baseNumber){
        this.domain = domain;
        this.generator = new IDGenerator(baseNumber);
        this.messages = new HashMap<>();
    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg) {
        Log.info("PostMessage: user=" + user + " ; pwd=" + pwd + " ; msg=" + msg);
        var address = Formatter.getUserAddress(user);

        if(! this.domain.equals(address.domain()) || this.domain.equals(msg.getDomain())
               || pwd == null || msg.getDomain() == null || msg.getUser() == null ){
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        var ds = Discovery.getInstance();
        URI[] serverURIs = ds.knownUrisOf(Formatter.getServiceID(this.domain, Formatter.USERS_SERVICE), 1);

        if(serverURIs.length == 0){
            System.out.println("Unable to contact the user server.");
            return Result.error( ErrorCode.TIMEOUT );
        }

        // optmize later :)
        Users usersServer = UsersClientFactory.get(serverURIs[0]);
        if(! usersServer.verifyPassword(address.username(), pwd).isOK() ){
            System.out.println("User doesn't exit.");
            return Result.error( ErrorCode.NOT_FOUND);
        }

        synchronized (messages){
            var userMessages = messages.computeIfAbsent(user, k -> new HashMap<>());
            long mid = generator.nextID();
            msg.setId(mid);
            // msg.setDomain(domain);
            userMessages.put(mid, msg);
            return Result.ok(mid);
        }
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
