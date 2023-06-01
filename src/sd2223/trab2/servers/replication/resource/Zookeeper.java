package sd2223.trab2.servers.replication.resource;

import org.apache.zookeeper.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;


public class Zookeeper implements Watcher {

	static final String SERVERS = "zookeeper";
	private ZooKeeper _client;
	private final int TIMEOUT = 5000;
	private String rootAppID;

	public Zookeeper(String rootNode) throws Exception {
		this.connect(SERVERS, TIMEOUT);
	}

	public synchronized ZooKeeper client() {
		if (_client == null || !_client.getState().equals(ZooKeeper.States.CONNECTED)) {
			throw new IllegalStateException("ZooKeeper is not connected.");
		}
		return _client;
	}

	private void connect(String host, int timeout) throws IOException, InterruptedException {
		var connectedSignal = new CountDownLatch(1);
		_client = new ZooKeeper(host, TIMEOUT, (e) -> {
			if (e.getState().equals(Event.KeeperState.SyncConnected)) {
				connectedSignal.countDown();
			}
		});
		connectedSignal.await();
	}

	public String createNode(String path, byte[] data, CreateMode mode) {
		try {
			return client().create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
		} catch (KeeperException.NodeExistsException x) {
			return path;
		} catch (Exception x) {
			x.printStackTrace();
			return null;
		}
	}

	public List<String> getChildren(String path) {
		try {
			return client().getChildren(path, false);
		} catch (Exception x) {
			x.printStackTrace();
		}
		return Collections.emptyList();
	}

	public List<String> getChildren(String path, Watcher watcher) {
		try {
			return client().getChildren(path, watcher);
		} catch (Exception x) {
			x.printStackTrace();
		}
		return Collections.emptyList();
	}

	public void createRootNode(){
		this.createNode(this.rootAppID, new byte[0], CreateMode.PERSISTENT);
	}

		/*

	ZookeeperNode {
		Zookeeper( .... ){};
		RepServerInfo getPrimary();
		Long getNodeId()
		List<RepServerInfo> getServers();
		boolean isConnected();
		State getCurrentState(); -> Disconnected | primary | Secondary
	}

	RepResource {
		boolean isPrimary = false;

		 ....
		 if(is_primary) {
		 	if(!zk.isConnected()) {
		 		// error
		 	}
		 } else {
		 	var primary = zk.getPrimary();
			 if(primary.serverID() == zk.getNodeId()) {
			 	// I just become the primary :)
			 } else {
			 	 forward(primary.serverURI());
			 }

		 }

	}

	record RepServerInfo(Long serverID, URI severURI){};



	public String getPrimaryID(){
		long min    = Long.MAX_VALUE;
		var min_str = "";
		for(var node : this.getChildren(APP_ROOT) ){
			var aux = Long.parseLong( node );
			if( aux < min ){
				min = aux;
				min_str = node;
			}
		}
		return min_str;
	}
		 */

	@Override
	public void process(WatchedEvent event) {
		switch (event.getType()){
			case NodeCreated -> System.out.println("new node: "+ event.getPath());
			case NodeDeleted -> System.out.println("node died:" + event.getPath());
			default -> {
				System.out.println("something happened: " + event);
			}
		}
	}
}
