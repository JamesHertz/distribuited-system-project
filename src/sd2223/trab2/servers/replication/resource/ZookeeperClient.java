package sd2223.trab2.servers.replication.resource;

import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;


public class ZookeeperClient implements Watcher {
    static {
        // zookeeper logs off :)
        System.setProperty("org.slf4j.simpleLogger.log.org.apache.zookeeper.ZooKeeper", "off");
        System.setProperty("org.slf4j.simpleLogger.log.org.apache.zookeeper.ClientCnxn", "off");
        System.setProperty("org.slf4j.simpleLogger.log.org.apache.zookeeper.common.X509Util", "off");
        System.setProperty("org.slf4j.simpleLogger.log.org.apache.zookeeper.ClientCnxnSocket", "off");
    }

    private static final String SERVERS = "kafka";
    private static final int TIMEOUT = 5000;
    private static final Logger Log = LoggerFactory.getLogger(ZookeeperClient.class);


    private final String rootNode;
    private final byte[] serverURI;
    private List<RepServerInfo> cache;
    private final Consumer<ZookeeperClient> callback;

    private ZooKeeper _client;
    private RepServerInfo primaryNode;
    private long serverID;
    private boolean networkErrors; // true when this node is having connections issues with zookeeper

    public ZookeeperClient(String serviceID, String serverURI, Consumer<ZookeeperClient> callback) {
        this.rootNode = "/" + serviceID; // <domain>:[ feeds | users ]
        this.serverURI = serverURI.getBytes(Charset.defaultCharset());
        this.callback = callback; // function that will be called when a node becomes primary
        this.cache = new ArrayList<>(); // save all the current known nodes along with their URI
        this.networkErrors = false; // is true when the node is having connections problem with Zookeeper

        this.startNode();
    }

    public synchronized ZooKeeper client() {
        if (this.isDisconnected()) {
            throw new IllegalStateException("ZooKeeper is not connected.");
        }
        return _client;
    }

    public synchronized State getState() {
        if (this.isDisconnected()) return State.DISCONNECTED;
        if (primaryNode != null && primaryNode.serverID == this.serverID) return State.PRIMARY;
        return State.OTHER;
    }

    public synchronized RepServerInfo getPrimaryNode() {
        return this.primaryNode;
    }

    public Long getServerID() {
        return this.serverID;
    }

    // all servers information except its own
    public List<RepServerInfo> getServers() {
        return cache;
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getState()) {
            case Disconnected -> {
                Log.error("service temporarily unavailable");
                this.networkErrors = true;
            }
            case SyncConnected -> {
                switch (watchedEvent.getType()) {
                    case NodeChildrenChanged -> {
                        this.update();
                    }
                    case None -> {
                        Log.info("We are back to live :)");
                        this.networkErrors = false;
                    }
                }
            }
            case Expired -> {
                Log.error("Section expired");
                this.networkErrors = false;
                this.reconnect();
            }
        }
    }

    private void startNode() {
        try {
            this.connect();
            this.createNode(
                    this.rootNode, new byte[0], CreateMode.PERSISTENT
            ); // creat root node :)
            this.launchNode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connect() throws IOException, InterruptedException {
        var connectedSignal = new CountDownLatch(1);
        _client = new ZooKeeper(SERVERS, TIMEOUT, (e) -> {
            if (e.getState().equals(Event.KeeperState.SyncConnected)) {
                connectedSignal.countDown();
            }
        });
        connectedSignal.await();
    }

    private String createNode(String path, byte[] data, CreateMode mode) {
        try {
            return client().create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
        } catch (KeeperException.NodeExistsException x) {
            return path;
        } catch (Exception x) {
            x.printStackTrace();
            return null;
        }
    }

    private void launchNode() {
        var res = this.createNode(this.rootNode + "/", serverURI, CreateMode.EPHEMERAL_SEQUENTIAL);
        serverID = Long.parseLong(res.split("/")[2]); // res= /<serviceID>/000..
        Log.info("Zookeeper node running: {}", serverID);
        this.update();
    }

    private synchronized void update() {
        try {
            Log.info("Updating ...");
            var prevState = this.getState();
            long min = Long.MAX_VALUE;
            var newCache = new ArrayList<RepServerInfo>();
            for (var server : client().getChildren(this.rootNode, this)) {
                var data = client().getData(this.rootNode + "/" + server, false, null);
                var node = new RepServerInfo(Long.parseLong(server), URI.create(new String(data)));

                if (node.serverID != this.serverID)
                    newCache.add(node);

                if (node.serverID < min) {
                    primaryNode = node;
                    min = node.serverID;
                }
            }
            this.cache = newCache;
            if (prevState == State.OTHER
                    && this.getState() == State.PRIMARY) { // become a primary
                Log.info("I am the new primary :)");
                callback.accept(this);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private boolean isDisconnected() {
        return networkErrors || _client == null || !_client.getState().equals(ZooKeeper.States.CONNECTED);
    }

    private void reconnect() {
        _client = null;
        for (; ; ) {
            try {
                Log.info("Trying to reconnect....");
                Thread.sleep(TIMEOUT);
                this.connect();
                this.launchNode();
                break;
            } catch (IOException | InterruptedException e) {
                Log.error("Failed :)");
                e.printStackTrace();
            }

        }
    }

    // state that a node can be at
    // PRIMARY      - it is the primary node which can execute write operations
    // OTHER        - secondary node only executes read operations with version below its current version
    //                or write operations that comes from the primary
    // DISCONNECTED - node is having network issues connecting to the zookeeper
    public enum State {
        PRIMARY, OTHER, DISCONNECTED;
    }

    public record RepServerInfo(long serverID, URI severURI) { }

}
