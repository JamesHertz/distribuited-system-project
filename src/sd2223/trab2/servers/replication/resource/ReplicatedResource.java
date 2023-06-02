package sd2223.trab2.servers.replication.resource;

import jakarta.ws.rs.WebApplicationException;
import static jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response;
import sd2223.trab2.api.Message;
import static  sd2223.trab2.api.Operations.*;

import sd2223.trab2.api.Operations;
import sd2223.trab2.api.Update;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.api.replication.ReplicatedFeedsService;
import sd2223.trab2.clients.ClientFactory;
import sd2223.trab2.servers.rest.resources.RestResource;
import sd2223.trab2.utils.JSON;
import sd2223.trab2.utils.Secret;

import static  sd2223.trab2.servers.replication.ReplicatedServer.VersionProvider;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class ReplicatedResource  extends RestResource implements ReplicatedFeedsService, VersionProvider{
    private final Feeds impl;
    private final ZookeeperClient zk;
    private long version = 0L;

    // simplificar -> version <= current
    public ReplicatedResource(Feeds impl, String serviceID, URI serverURI) throws  Exception{
        this.impl = impl;
        this.zk = new ZookeeperClient(serviceID, serverURI.toString(), w -> {
            System.out.println("doing something fun :)");
        });
    }

    private  <T> T fromJavaResult(Supplier<Result<T>> supplier) {
        return switch (this.zk.getState()){
            case PRIMARY -> super.fromJavaResult( supplier.get() );
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
        return this.fromJavaResult( () -> {
            Update up = Update.toUpdate(
                    CREATE_MESSAGE, user, pwd, JSON.encode(msg)
            );
            CountDownLatch cd = new CountDownLatch(1); // todo: look at this later
            var errors = new ConcurrentLinkedDeque<Result<?>>();
            for(var server : this.zk.getServers()){
                if(server.serverID() == this.zk.getServerID()) continue;
                new Thread( () -> {
                    var client = ClientFactory.getReplicatedClient(server.severURI(), this);
                    errors.add(
                            client.update(Secret.getSecret(), up)
                    );
                    cd.countDown();
                }).start();
            }
            try{
                cd.await();
            }catch (InterruptedException ignore){ }

            for(var err : errors){
                if(err.isOK())
                    return impl.postMessage(user, pwd, msg);
                if (err.error() != Result.ErrorCode.TIMEOUT )
                    return Result.error( err.error() );
            }
            return Result.error( Result.ErrorCode.SERVICE_UNAVAILABLE );
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
    public void update(Long version, String secret, Update update) {
        if(!secret.equals(Secret.getSecret()))
            throw new WebApplicationException(Status.UNAUTHORIZED);
        if(this.getCurrentVersion() == version){
            var operation = Operations.valueOf(update.getOperation());
            var args = update.getArgs();
            // TODO: add the number of arguments of each operation
            // TODO: add a try/catch that if we get an error return badrequest
            if(operation == null) throw new WebApplicationException( Status.BAD_REQUEST );

            switch (operation){
                case CREATE_MESSAGE -> impl.postMessage(args[0], args[1], JSON.decode(args[2], Message.class));
                default -> {
                    System.out.println("whatever");
                }
            }
        }else {
            // what to do?
        }
    }

    @Override
    public List<Update> getOperations(Long version, String secret) {
        return null;
    }


    @Override
    public synchronized long getCurrentVersion() {
        return this.version;
    }

}
