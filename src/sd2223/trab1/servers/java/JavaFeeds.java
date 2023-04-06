package sd2223.trab1.servers.java;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.clients.UsersClientFactory;
import sd2223.trab1.discovery.Discovery;
import sd2223.trab1.utils.Formatter;
import sd2223.trab1.utils.IDGenerator;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import static sd2223.trab1.api.java.Result.ErrorCode;

// next test 3b :)
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

        if( address == null || ! this.domain.equals(address.domain())
               || pwd == null || msg.getDomain() == null || msg.getUser() == null  ){
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Users usersServer = getDomainUserServer();

        if(usersServer == null ){
            System.out.println("Unable to contact the user server.");
            return Result.error( ErrorCode.TIMEOUT );
        }

        // optmize later :)
        Result<Void> err;
        if(! ( err = usersServer.verifyPassword(address.username(), pwd) ).isOK() ){
            System.out.println("User or password incorrect.");
            return Result.error( err.error() );
        }

        synchronized (messages){
            var userMessages = messages.computeIfAbsent(user, k -> new HashMap<>());
            long mid = generator.nextID();
            msg.setId(mid);
            msg.setDomain(domain);
            msg.setCreationTime(System.currentTimeMillis());
            userMessages.put(mid, msg);
            return Result.ok(mid);
        }
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        var address = Formatter.getUserAddress(user);

        if( address == null || ! this.domain.equals(address.domain()) || pwd == null ){
            Log.info("Invalid user, domain or password.");
           return Result.error( ErrorCode.BAD_REQUEST );
        }

        var usersServer = this.getDomainUserServer();
        if(usersServer == null){
            Log.info("Unable to connect user server.");
            return Result.error( ErrorCode.TIMEOUT );
        }

        Result<Void> err;
        if( ! (err = usersServer.verifyPassword(address.username(), pwd ) ).isOK() ){
            Log.info("Wrong username or password.");
            return Result.error( err.error() );
        }

        Map<Long, Message> userMsgs;
        synchronized (messages){
            userMsgs = messages.get(user);
        }

        synchronized (userMsgs){
            if(userMsgs.remove(mid) == null){
                Log.info("Message not found.");
                return Result.error( ErrorCode.NOT_FOUND);
            }
        }

        return Result.error(ErrorCode.NO_CONTENT);
    }

    @Override
    public Result<Message> getMessage(String user, long mid) {

        Map<Long, Message> userMsgs;
        Message res = null;

        synchronized (messages){
            userMsgs = messages.get(user);
        }

        if(userMsgs != null){
            synchronized (userMsgs){
                res = userMsgs.get(mid);
            }
        }

        if(res == null){
            Log.info("Message doesn't exit.");
            return Result.error( ErrorCode.NOT_FOUND );
        }

        return Result.ok(res);
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        Map<Long, Message> msgs;

        synchronized (messages) {
            msgs = messages.get(user);
        }

        if(msgs == null){
             Log.info("Messages not found.");
             return Result.error( ErrorCode.NOT_FOUND );
        }

        synchronized (msgs){
            return Result.ok(
                   msgs.values()
                       .stream()
                       .filter( m -> m.getCreationTime() > time)
                       .toList()
            );
        }
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

    private Users getDomainUserServer(){
        var ds = Discovery.getInstance();
        URI[] serverURIs = ds.knownUrisOf(Formatter.getServiceID(this.domain, Formatter.USERS_SERVICE), 1);
        if(serverURIs.length == 0) return null;
        return UsersClientFactory.get(serverURIs[0]);
    }
}
