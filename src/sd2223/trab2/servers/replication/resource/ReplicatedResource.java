package sd2223.trab2.servers.replication.resource;

import jakarta.ws.rs.WebApplicationException;

import static jakarta.ws.rs.core.Response.Status;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.server.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sd2223.trab2.api.Message;

import static sd2223.trab2.api.Operations.*;

import sd2223.trab2.api.Operations;
import sd2223.trab2.api.Update;
import sd2223.trab2.api.java.RepFeeds;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.api.replication.ReplicatedFeedsService;
import sd2223.trab2.clients.ClientFactory;
import sd2223.trab2.servers.java.JavaUsers;
import sd2223.trab2.servers.replication.ReplicatedServer;
import sd2223.trab2.servers.rest.resources.RestResource;
import sd2223.trab2.utils.JSON;
import sd2223.trab2.utils.Secret;

import static sd2223.trab2.servers.replication.ReplicatedServer.VersionProvider;
import static sd2223.trab2.api.java.Result.ErrorCode;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

// next: 108a
/*
 TODO
    - change client to choose a random URI
    - verify that the update came from the actual primary
    - just do updateToLastVersion if you are in the state other (?)
 */
public class ReplicatedResource extends RestResource implements ReplicatedFeedsService, VersionProvider {
    static {
        System.setProperty("org.slf4j.simpleLogger.log." + ReplicatedServer.class.getName(), "debug");
    }

    private static final int FAILURE_TOLERANCE = 1;
    private static final int REPLICAS_NUMBER = 2 * FAILURE_TOLERANCE + 1;
    private static final int REQUIRED_CONFIRMATIONS = REPLICAS_NUMBER / 2;

    private final RepFeeds impl;
    private final ZookeeperClient zk;
    private final AtomicLong version;
    private final List<Update> operations;

    private static final Logger Log = LoggerFactory.getLogger(ReplicatedServer.class);

    // simplificar -> version <= current
    public ReplicatedResource(RepFeeds impl, String serviceID, URI serverURI) throws Exception {
        this.impl = impl;
        this.operations = new ArrayList<>(100);
        this.version = new AtomicLong(0);
        this.zk = new ZookeeperClient(serviceID, serverURI.toString(), w -> {
            // if( servers.size() > REQUIRED_CONFIRMATIONS )
            this.updateToMostRecentVersion();
            System.out.println("doing something fun :)");
        });
        Log.info("Server running...");
    }


    @Override
    public long postMessage(ContainerRequest request, String user, String pwd, Message msg) {
        Log.info("postMessage: version={} ; user={} ; pwd={}; msg={}", version, user, pwd, msg);

        // TODO: verify values here
        if (msg != null) {
            msg.setCreationTime(System.currentTimeMillis());
            msg.setId(impl.getGenerator().nextID());
        }

        return this.executeWriteOperation(
                () -> impl.postMessage(user, pwd, msg),
                Update.toUpdate(
                        CREATE_MESSAGE, user, pwd, JSON.encode(msg)
                ),
                request
        );
    }

    @Override
    public void removeFromPersonalFeed(ContainerRequest request, String user, long mid, String pwd) {
        Log.info("removeFromPersonalFeed: version: {} ; user: {} ; mid: {} ; pwd: {}", version, user, mid, pwd);
        this.executeWriteOperation(
                () -> impl.removeFromPersonalFeed(user, mid, pwd),
                Update.toUpdate(
                        REMOVE_FROM_FEED, user, mid, pwd
                ),
                request
        );
    }

    @Override
    public Message getMessage(Long version, String user, long mid) {
        // @Context  HttpHeaders headers (use this c:)
        return this.executeReadOperation(version, () -> impl.getMessage(user, mid));
    }

    @Override
    public List<Message> getMessages(Long version, String user, long time) {
        Log.info("getMessages: version={} ; user={} ; time={}", version, user, time);
        return this.executeReadOperation(version, () -> impl.getMessages(user, time));
    }

    @Override
    public void subUser(ContainerRequest request, String user, String userSub, String pwd) {
        this.executeWriteOperation(
                () -> impl.subscribeUser(user, userSub, pwd),
                Update.toUpdate(
                        SUBSCRIBE_USER, user, userSub, pwd
                ),
                request
        );
    }

    @Override
    public void unsubscribeUser(ContainerRequest request, String user, String userSub, String pwd) {
        this.executeWriteOperation(
                () -> impl.unSubscribeUser(user, userSub, pwd),
                Update.toUpdate(
                        UNSUBSCRIBE_USER, user, userSub, pwd
                ),
                request
        );
    }

    @Override
    public List<String> listSubs(Long version, String user) {
        return this.executeReadOperation(version, () -> impl.listSubs(user));
    }

    @Override
    public void createFeed(ContainerRequest request, String user, String secret) {
        Log.info("creatFeed: user={} ; secret={}", user, secret);
        this.executeWriteOperation(
                () -> impl.createFeed(user, secret),
                Update.toUpdate(
                        CREATE_FEED, user, secret
                ),
                request
        );
    }

    @Override
    public void removeFeed(ContainerRequest request, String user, String secret) {
        this.executeWriteOperation(
                () -> impl.removeFeed(user, secret),
                Update.toUpdate(
                        REMOVE_FEED, user, secret
                ),
                request
        );
    }

    @Override
    public List<Message> subscribeServer(ContainerRequest request, String domain, String user, String secret) {
        return this.executeWriteOperation(
                () -> impl.subscribeServer(domain, user, secret),
                Update.toUpdate(
                        SUBSCRIBE_SERVER, domain, user, secret
                ),
                request
        );
    }

    @Override
    public void unsubscribeServer(ContainerRequest request, String domain, String user, String secret) {
        this.executeWriteOperation(
                () -> impl.unsubscribeServer(domain, user, secret),
                Update.toUpdate(
                        UNSUBSCRIBE_SERVER, domain, user, secret
                ),
                request
        );
    }

    @Override
    public void createExtFeedMessage(ContainerRequest request, String user, String secret, Message msg) {
        this.executeWriteOperation(
                () -> impl.createExtFeedMessage(user, secret, msg),
                Update.toUpdate(
                        CREATE_EXT_FEED_MSG, user, secret, JSON.encode(msg)
                ),
                request
        );
    }

    @Override
    public void removeExtFeedMessage(ContainerRequest request, String user, long mid, String secret) {
        this.executeWriteOperation(
                () -> impl.removeExtFeedMessage(user, mid, secret),
                Update.toUpdate(
                        REMOVE_EXT_FEED_MSG, user, mid, secret
                ),
                request
        );
    }

    @Override
    public void removeExtFeed(ContainerRequest request, String user, String secret) {
        this.executeWriteOperation(
                () -> impl.removeExtFeed(user, secret),
                Update.toUpdate(
                        REMOVE_EXT_FEED, user, secret
                ),
                request
        );
    }

    @Override
    public int update(Long version, String secret, Update update) {
        Log.info("update: version: {} ; secret: {}, update: {}", version, secret, update);
        if (!secret.equals(Secret.getSecret()))
            throw new WebApplicationException(Status.UNAUTHORIZED);

        if (this.getCurrentVersion() != version) // TODO: check if the update is coming from the primary
            this.updateToMostRecentVersion();

        if (this.getCurrentVersion() == version) { // if it worked
            var res = this.execute_operation(update);

            if (this.success(res))
                this.addOperation(update);


            Status status = res.isOK() ? Status.OK : RestResource.statusCodeFrom(res);
            Log.info("Operation status: {}", status);
            return status.getStatusCode();
        } else {
            throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public List<Update> getOperations(long version, String secret) {
        Log.info("getOperations: {} {}", version, secret);
        if (!Secret.getSecret().equals(secret))
            throw new WebApplicationException(Status.UNAUTHORIZED);

        if (version >= this.getCurrentVersion())
            throw new WebApplicationException(Status.NOT_FOUND);

        synchronized (operations) {
            Log.debug("operations.size(): {} ; this.getCurrentVersion(): {}", operations.size(), this.getCurrentVersion());
            var res =  operations.subList((int) version, operations.size());
            Log.info("updates sent: {}", res);
            return res;
        }
    }


    @Override
    public long getCurrentVersion() {
        return this.version.get();
    }

    private <T> T executeWriteOperation(Supplier<Result<T>> operation, Update update, ContainerRequest request) {
        return switch (this.zk.getState()) {
            case PRIMARY -> {
                Log.info("I am the primary and going to try to execute it!!");
                Result<T> res;
                if (this.canExecute(update)) {
                    Log.info("Executing request...");
                    res = operation.get(); // executes operation
                    if (this.success(res)) // save update
                        this.addOperation(update);

                } else {
                    res = Result.error(ErrorCode.SERVICE_UNAVAILABLE);
                    Log.info("Cannot execute request...");
                }
                // remove the last operation :)
                yield super.fromJavaResult(res);
            }
            case OTHER -> {
                Log.info("Redirecting to primary...");
                var originalURI = request.getUriInfo().getRequestUri();
                var primaryURI = this.zk.getPrimaryNode().severURI();
                throw new WebApplicationException(
                        Response.temporaryRedirect(
                                UriBuilder.fromUri(originalURI)
                                        .host(primaryURI.getHost())
                                        .build()
                        ).build()
                );
            }
            case DISCONNECTED -> {
                Log.info("Server disconnected...");
                throw new WebApplicationException(
                        Status.SERVICE_UNAVAILABLE
                );
            }
        };
    }

    private <T> T executeReadOperation(Long version, Supplier<Result<T>> supplier) {
        Log.info("executeReadOperation: version={} ; current_version={}", version, this.getCurrentVersion());
        if (version != null && version > this.getCurrentVersion()) {// check if primary :)
            Log.debug("Server is behind getting the most recent version.");
            this.updateToMostRecentVersion(); // try to get to the most recent version and then do something else :)
        }
        return super.fromJavaResult(
                version == null || version <= this.getCurrentVersion() ?
                        supplier.get() : Result.error(ErrorCode.SERVICE_UNAVAILABLE)
        );
    }

    private boolean canExecute(Update update) {
        var servers = this.zk.getServers();
        var request_nr = servers.size();

        if (request_nr < REQUIRED_CONFIRMATIONS) return false; // DO not execute when it's below this :)

        var errors = new ConcurrentLinkedDeque<Result<Integer>>();
        Semaphore sem = new Semaphore(0);

        for (var server : servers) {
            new Thread(() -> {
                var client = ClientFactory.getReplicatedClient(server.severURI(), this);
                errors.add(
                        client.update(Secret.getSecret(), update)
                );
                sem.release(); // inc
            }).start();
        }
        int conf = 0;
        while (request_nr > 0) {
            try {
                sem.acquire();
            } catch (InterruptedException ignore) { } // wait
            var err = errors.remove();
            if (err.isOK()) conf++; // they were able to execute the operation
            if (conf == REQUIRED_CONFIRMATIONS) return true;
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
            case CREATE_MESSAGE      -> impl.postMessage(args[0], args[1], JSON.decode(args[2], Message.class));
            case REMOVE_FROM_FEED    -> impl.removeFromPersonalFeed(args[0], Long.parseLong(args[1]), args[2]);
            case CREATE_FEED         -> impl.createFeed(args[0], args[1]);
            case REMOVE_FEED         -> impl.removeFeed(args[0], args[1]);
            case SUBSCRIBE_USER      -> impl.subscribeUser(args[0], args[1], args[2]);
            case UNSUBSCRIBE_USER    -> impl.unSubscribeUser(args[0], args[1], args[2]);
            case SUBSCRIBE_SERVER    -> impl.subscribeServer(args[0], args[1], args[2]);
            case UNSUBSCRIBE_SERVER  -> impl.unsubscribeServer(args[0], args[1], args[2]);
            case REMOVE_EXT_FEED     -> impl.removeExtFeed(args[0], args[1]);
            case CREATE_EXT_FEED_MSG -> impl.createExtFeedMessage(args[0], args[1], JSON.decode(args[2], Message.class));
            case REMOVE_EXT_FEED_MSG -> impl.removeExtFeedMessage(args[0], Long.parseLong(args[1]), args[2]);
        };
    }


    // TODO: find a better solution than this
    // TODO: make sure we are only doing one update per instance
    // TODO: think about states (by Iago)
    private void updateToMostRecentVersion() {
        Log.debug("Getting the most recent version");
        if (this.zk == null || this.zk.getServers().isEmpty()) {
            Log.debug("First call or no servers...");
            return;
        }
        var servers = this.zk.getServers();
        Log.debug("sending getOperation request for: {}", servers);
        var cd = new CountDownLatch(servers.size());
        var updates = new ConcurrentLinkedDeque<List<Update>>();
        for (var server : servers) {
            new Thread(() -> {
                var client = ClientFactory.getReplicatedClient(
                        server.severURI(), this
                );
                var res = client.getOperations(this.getCurrentVersion(), Secret.getSecret());
                if (res.isOK())
                    updates.add(res.value());
                cd.countDown();
            }).start();
        }
        try {
            cd.await();
        } catch (InterruptedException ignore) {
        }

        Log.debug("updates gotten: {}", updates);
        var mostRecent = updates.stream()
                .max(Comparator.comparingInt(List::size));

        if (mostRecent.isEmpty()) {
            Log.debug("Couldn't get any updates...");
            return;
        }

        Log.debug("most recent: {}", mostRecent);
        for (var up : mostRecent.get()) {
            this.execute_operation(up);  // execute
            this.addOperation(up); // save
        }
        Log.debug("updated to version: {}", this.getCurrentVersion());
    }

    private void addOperation(Update update) {
        synchronized (operations) {
            operations.add(update);
            this.version.incrementAndGet();
        }
    }

    private boolean success(Result<?> res) {
        return res.isOK() || res.error() == ErrorCode.NO_CONTENT;
    }
}
