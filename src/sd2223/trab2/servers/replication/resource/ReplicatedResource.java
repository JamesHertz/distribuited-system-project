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
import sd2223.trab2.api.java.RepFeeds;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.api.replication.ReplicatedFeedsService;
import sd2223.trab2.clients.ClientFactory;
import sd2223.trab2.servers.replication.ReplicatedServer;
import sd2223.trab2.servers.rest.resources.RestResource;
import sd2223.trab2.utils.JSON;
import sd2223.trab2.utils.Secret;

import static  sd2223.trab2.servers.replication.ReplicatedServer.VersionProvider;
import static  sd2223.trab2.api.java.Result.ErrorCode;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class ReplicatedResource  extends RestResource implements ReplicatedFeedsService, VersionProvider{
    private static final int FAILURE_TOLERANCE      =  1;
    private static final int REPLICAS_NUMBER        = 2 * FAILURE_TOLERANCE + 1;
    private static final int REQUIRED_CONFIRMATIONS = REPLICAS_NUMBER / 2;

    private final RepFeeds impl;
    private final ZookeeperClient zk;
    private final AtomicLong version;

    private static final Logger Log = LoggerFactory.getLogger(ReplicatedServer.class);

    // simplificar -> version <= current
    public ReplicatedResource(RepFeeds impl, String serviceID, URI serverURI) throws  Exception{
        this.impl    = impl;
        this.version = new AtomicLong(0);
        this.zk = new ZookeeperClient(serviceID, serverURI.toString(), w -> {
            System.out.println("doing something fun :)");
        });
        Log.info("Server running...");
    }


    @Override
    public long postMessage(Long version, String user, String pwd, Message msg) {
        Log.info("postMessage: version={} ; user={} ; pwd={}; msg={}", version, user, pwd, msg);

        // TODO: verify values here
        if(msg != null){
            msg.setCreationTime(System.currentTimeMillis());
            msg.setId(impl.getGenerator().nextID());
        }

        return this.executeWriteOperation(
                () -> impl.postMessage(user, pwd, msg),
                Update.toUpdate(
                        CREATE_MESSAGE, user, pwd, JSON.encode(msg)
                )
        );
    }

    @Override
    public void removeFromPersonalFeed(Long version, String user, long mid, String pwd) {
        Log.info("removeFromPersonalFeed: version: {} ; user: {} ; mid: {} ; pwd: {}", version, user, mid, pwd);
        this.executeWriteOperation(
                () -> impl.removeFromPersonalFeed(user, mid, pwd),
                Update.toUpdate(
                    REMOVE_FROM_FEED, user, mid, pwd
                )
        );
    }

    @Override
    public Message getMessage(Long version, String user, long mid) {
        return this.executeReadOperation(version, () -> impl.getMessage(user, mid));
    }

    @Override
    public List<Message> getMessages(Long version, String user, long time) {
        return this.executeReadOperation(version, () -> impl.getMessages(user, time));
    }

    @Override
    public void subUser(Long version, String user, String userSub, String pwd) {
        this.executeWriteOperation(
                () -> impl.subscribeUser(user, userSub, pwd),
                Update.toUpdate(
                        SUBSCRIBE_USER, user, userSub, pwd
                )
        );
    }

    @Override
    public void unsubscribeUser(Long version, String user, String userSub, String pwd) {
        this.executeWriteOperation(
                () -> impl.unSubscribeUser(user, userSub, pwd),
                Update.toUpdate(
                        UNSUBSCRIBE_USER, user, userSub, pwd
                )
        );
    }

    @Override
    public List<String> listSubs(Long version, String user) {
        return this.executeReadOperation(version, () -> impl.listSubs(user));
    }

    @Override
    public void createFeed(Long version, String user, String secret) {
        this.executeWriteOperation(
                () -> impl.createFeed(user, secret),
                Update.toUpdate(
                        CREATE_FEED, user, secret
                )
        );
    }

    @Override
    public void removeFeed(Long version, String user, String secret) {
        this.executeWriteOperation(
                () -> impl.removeFeed(user, secret),
                Update.toUpdate(
                        REMOVE_FEED,user, secret
                )
        );
    }

    @Override
    public List<Message> subscribeServer(Long version, String domain, String user, String secret) {
        return this.executeWriteOperation(
                () -> impl.subscribeServer(domain, user, secret),
                Update.toUpdate(
                        SUBSCRIBE_SERVER, domain, user, secret
                )
        );
    }

    @Override
    public void unsubscribeServer(Long version, String domain, String user, String secret) {
        this.executeWriteOperation(
                () -> impl.unsubscribeServer(domain, user, secret),
                Update.toUpdate(
                        UNSUBSCRIBE_SERVER, domain, user, secret
                )
        );
    }

    @Override
    public void createExtFeedMessage(Long version, String user, String secret, Message msg) {
        this.executeWriteOperation(
                () -> impl.createExtFeedMessage(user, secret, msg),
                Update.toUpdate(
                        CREATE_EXT_FEED_MSG, user, secret, JSON.encode(msg)
                )
        );
    }

    @Override
    public void removeExtFeedMessage(Long version, String user, long mid, String secret) {
        this.executeWriteOperation(
                () -> impl.removeExtFeedMessage(user, mid, secret),
                Update.toUpdate(
                        REMOVE_EXT_FEED_MSG, user, mid, secret
                )
        );
    }

    @Override
    public void removeExtFeed(Long version, String user, String secret) {
        this.executeWriteOperation(
                () -> impl.removeExtFeed(user, secret),
                Update.toUpdate(
                        REMOVE_EXT_FEED, user, secret
                )
        );
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
        } else {
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
    public long getCurrentVersion() {
        return this.version.get();
    }

    private  <T> T executeWriteOperation(Supplier<Result<T>> operation, Update update) {
        return switch (this.zk.getState()){
            case PRIMARY -> {
                Result<T> res;
                if(this.canExecute(update)){
                    res = operation.get(); // executes operation
                    if(res.isOK()){
                        version.incrementAndGet();
                        // save update
                    }
                } else
                    res = Result.error( ErrorCode.SERVICE_UNAVAILABLE );
                // remove the last operation :)
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

    private <T> T executeReadOperation(Long version, Supplier<Result<T>> supplier){
        return super.fromJavaResult(
                version == null || version <= this.version.get() ?
                        supplier.get() : Result.error( ErrorCode.SERVICE_UNAVAILABLE )
        );
    }

    private boolean canExecute(Update update){
        var errors = new ConcurrentLinkedDeque<Result<Integer>>();
        var servers = this.zk.getServers();
        var request_nr = servers.size() - 1;
        Semaphore sem = new Semaphore(0);

        for(var server : servers){
            if(server.serverID() == this.zk.getServerID()) continue;
            new Thread( () -> {
                var client = ClientFactory.getReplicatedClient(server.severURI(), this);
                errors.add(
                        client.update(Secret.getSecret(), update)
                );
                sem.release(); // inc
            }).start();
        }
        int conf = 0;
        while(request_nr > 0){
            try{ sem.acquire(); }catch (InterruptedException ignore){ } // wait
            var err = errors.remove();
            if( err.isOK() ) conf++; // they were able to execute the operation
            if(conf == REQUIRED_CONFIRMATIONS) return true;
            request_nr--;
        }
        return false;
    }

    private Result<?> execute_operation(Update update) {
        Operations operation;
        if (update == null || (operation = Operations.valueOf(update.getOperation())) == null)
            throw new WebApplicationException(Status.BAD_REQUEST);

        // we are assuming that from here below everything will be alright which may not be the case
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
            case REMOVE_EXT_FEED_MSG -> impl.removeExtFeedMessage(args[0], Long.parseLong(args[1]), args[2]);
        };
    }

}
