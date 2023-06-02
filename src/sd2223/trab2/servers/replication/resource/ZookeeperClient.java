package sd2223.trab2.servers.replication.resource;

import org.apache.zookeeper.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;


public class ZookeeperClient {
	private static final String SERVERS = "zookeeper";
	private static final int TIMEOUT = 5000;

	// ...
	private final String rootNode;
	private final byte[] serverURI;
	private final List<RepServerInfo> cache;
	private final Consumer<ZookeeperClient> callback;

	// ...
	private ZooKeeper _client;
	private RepServerInfo primaryNode;
	private long serverID;

	public ZookeeperClient(String rootNode, String serverURI, Consumer<ZookeeperClient> callback) {
		this.rootNode = rootNode;
		this.serverURI = serverURI.getBytes(Charset.defaultCharset());
		this.callback = callback;
		this.cache = new ArrayList<>();
	}

	public void startNode(){
		try{ // TODO: background thread
			this.connect();
			this.createRootNode();
			this.createServerNode();
			this.update();
		}catch (Exception e){
			e.printStackTrace();
		}

		//
	}

	public synchronized ZooKeeper client() {
		if (this.isDisconnected()) {
			throw new IllegalStateException("ZooKeeper is not connected.");
		}
		return _client;
	}

	public State getState(){
		if(this.isDisconnected()) return State.DISCONNECTED;
		if(primaryNode != null && primaryNode.serverID == this.serverID) return State.PRIMARY;
		return State.OTHER;
	}

	public RepServerInfo getPrimaryNode(){
		return this.primaryNode;
	}

	public Long getNodeId(){
		return this.serverID;
	}

	public List<RepServerInfo> getServers(){
		return cache;
	}

	public record RepServerInfo(Long serverID, URI severURI) { };

	private void connect() throws IOException, InterruptedException {
		var connectedSignal = new CountDownLatch(1);
		_client = new ZooKeeper(SERVERS, TIMEOUT, (e) -> {
			if (e.getState().equals(Watcher.Event.KeeperState.SyncConnected)) {
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

	private void createServerNode(){
		var res = this.createNode(this.rootNode + "/", serverURI, CreateMode.EPHEMERAL_SEQUENTIAL);
		// TODO: think about this
		serverID = Long.parseLong( res.split("/")[2] );
	}
	void createRootNode(){
		this.createNode(this.rootNode, new byte[0], CreateMode.PERSISTENT);
	}

	private void update(){
		try {
			System.out.println("updating...");
			var prevState = this.getState();
			long min = Long.MAX_VALUE;
			cache.clear(); // think about this :)

			for (var server : client().getChildren(this.rootNode, w -> this.update() )) {
				var data = client().getData(this.rootNode + "/" + server, false, null);
				var node =  new RepServerInfo( Long.parseLong(server), URI.create( new String( data ) ));

				cache.add(node);
				if(node.serverID < min){
					primaryNode = node;
					min = node.serverID;
				}
			}

			if(prevState == State.OTHER
					&& this.getState() == State.PRIMARY){ // become a primary
				callback.accept(this);
			}

		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	private boolean isDisconnected(){
		return _client == null || !_client.getState().equals(ZooKeeper.States.CONNECTED);
	}

	public enum State{
		PRIMARY, OTHER, DISCONNECTED;
	}
}
