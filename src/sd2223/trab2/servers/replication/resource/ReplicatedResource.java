package sd2223.trab2.servers.replication.resource;

import jakarta.ws.rs.WebApplicationException;
import static jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sd2223.trab2.api.Message;
import static  sd2223.trab2.api.Operations.*;

import sd2223.trab2.api.Operations;
import sd2223.trab2.api.Update;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.api.replication.ReplicatedFeedsService;
import sd2223.trab2.clients.ClientFactory;
import sd2223.trab2.servers.replication.ReplicatedServer;
import sd2223.trab2.servers.rest.resources.RestResource;
import sd2223.trab2.utils.JSON;
import sd2223.trab2.utils.Secret;

import static  sd2223.trab2.servers.replication.ReplicatedServer.VersionProvider;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class ReplicatedResource  extends RestResource implements ReplicatedFeedsService, VersionProvider{
    private final Feeds impl;
    private final ZookeeperClient zk;
    private AtomicLong version;

    private static final Logger Log = LoggerFactory.getLogger(ReplicatedServer.class);

    // simplificar -> version <= current
    public ReplicatedResource(Feeds impl, String serviceID, URI serverURI) throws  Exception{
        this.impl    = impl;
        this.version = new AtomicLong(0);
        this.zk = new ZookeeperClient(serviceID, serverURI.toString(), w -> {
            System.out.println("doing something fun :)");
        });
        Log.info("Server running...");
    }

    private  <T> T fromJavaResult(Supplier<Result<T>> supplier) {
        return switch (this.zk.getState()){
            case PRIMARY -> {
                var res = supplier.get();
                if(res.isOK()) version.incrementAndGet();
                yield super.fromJavaResult( res );
            }
            case OTHER -> throw new WebApplicationException(
                    Response.temporaryRedirect(
                            this.zk.getPrimaryNode().severURI()
                    ).build()
            );
            case DISCONNECTED -> throw new WebApplicationException(
                    Status.SERVICE_UNAVAILABLE
            );
        };
    }

    @Override
    public long postMessage(Long version, String user, String pwd, Message msg) {
        Log.info("postMessage: version={} ; user={} ; pwd={}; msg={}", version, user, pwd, msg);
        return this.fromJavaResult( () -> {
            Update up = Update.toUpdate(
                    CREATE_MESSAGE, user, pwd, JSON.encode(msg)
            );

            // TODO: store operation in a list of operations
            return this.canExecute(up) ?  impl.postMessage(user, pwd, msg)
                    :  Result.error( Result.ErrorCode.SERVICE_UNAVAILABLE );
        });
    }

    @Override
    public void removeFromPersonalFeed(Long version, String user, long mid, String pwd) {

    }

    @Override
    public Message getMessage(Long version, String user, long mid) {
        //Verificar versao...
        return null;
    }

    @Override
    public List<Message> getMessages(Long version, String user, long time) {
        return null;
    }

    @Override
    public void subUser(Long version, String user, String userSub, String pwd) {

    }

    @Override
    public void unsubscribeUser(Long version, String user, String userSub, String pwd) {

    }

    @Override
    public List<String> listSubs(Long version, String user) {
        return null;
    }

    @Override
    public void createFeed(Long version, String user, String secret) {

    }

    @Override
    public void removeFeed(Long version, String user, String secret) {

    }

    @Override
    public List<Message> subscribeServer(Long version, String domain, String user, String secret) {
        return null;
    }

    @Override
    public void unsubscribeServer(Long version, String domain, String user, String secret) {

    }

    @Override
    public void createExtFeedMessage(Long version, String user, String secret, Message msg) {

    }

    @Override
    public void removeExtFeedMessage(Long version, String user, long mid, String secret) {

    }

    @Override
    public void removeExtFeed(Long version, String user, String secret) {

    }

    @Override
    public int update(Long version, String secret, Update update) {
        Log.info("update: version: {} ; secret: {}, update: {}", version, secret, update);
        if(!secret.equals(Secret.getSecret()))
            throw new WebApplicationException(Status.UNAUTHORIZED);

        if(this.getCurrentVersion() == version){
            var res = this.execute_operation(update);
            Status status;

            if( res.isOK() ) {
                this.version.incrementAndGet();
                status = Status.OK;
            } else
                status = RestResource.statusCodeFrom( res );

            Log.info("Operation status: {}", status);
            return status.getStatusCode();
        }else {
            // TODO: handle this :)
            throw new WebApplicationException( Status.SERVICE_UNAVAILABLE );
        }
    }

    @Override
    public List<Update> getOperations(Long version, String secret) {
        Log.info("getOperations: {} {}", version, secret);
        return null;
    }


    @Override
    public synchronized long getCurrentVersion() {
        return this.version.get();
    }

    private boolean canExecute(Update update){
        var errors = new ConcurrentLinkedDeque<Result<?>>();
        CountDownLatch cd = new CountDownLatch(1); // todo: look at this later
        for(var server : this.zk.getServers()){
            if(server.serverID() == this.zk.getServerID()) continue;
            new Thread( () -> {
                var client = ClientFactory.getReplicatedClient(server.severURI(), this);
                errors.add(
                        client.update(Secret.getSecret(), update)
                );
                cd.countDown();
            }).start();
        }
        try{
            cd.await();
        }catch (InterruptedException ignore){ }

        for(var err : errors){
            if(err.isOK()) return true;
        }
        return false;
    }

    private Result<?> execute_operation(Update update) {
        Operations operation;
        if (update == null || (operation = Operations.valueOf(update.getOperation())) == null)
            throw new WebApplicationException(Status.BAD_REQUEST);

        var args = update.getArgs();
        return switch (operation) {
            case CREATE_MESSAGE   -> impl.postMessage(args[0], args[1], JSON.decode(args[2], Message.class));
            case REMOVE_FROM_FEED -> impl.removeFromPersonalFeed(args[0], Long.parseLong(args[1]), args[2]);
            case CREATE_FEED -> impl.createFeed(args[0], args[1]);
            case REMOVE_FEED -> impl.removeFeed(args[0], args[1]);
            case SUBSCRIBE_USER   -> impl.subscribeUser(args[0], args[1], args[2]);
            case UNSUBSCRIBE_USER -> impl.unSubscribeUser(args[0], args[1], args[2]);
            case SUBSCRIBE_SERVER -> impl.subscribeServer(args[0], args[1], args[2]);
            case UNSUBSCRIBE_SERVER -> impl.unsubscribeServer(args[0], args[1], args[2]);
            case REMOVE_EXT_FEED  -> impl.removeExtFeed(args[0], args[1]);
            case CREATE_EXT_FEED_MSG -> impl.createExtFeedMessage(args[0], args[1], JSON.decode(args[2], Message.class));
        };
    }
}
