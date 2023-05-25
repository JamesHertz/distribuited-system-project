package sd2223.trab2.discovery;

import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A class interface to perform service discovery based on periodic 
 * announcements over multicast communication.</p>
 * 
 */

public interface Discovery {
	/**
	 * Used to announce the URI of the given service name.
	 * @param serviceName - the name of the service
	 * @param serviceURI - the uri of the service
	 */
	public void announce(String serviceName, String serviceURI);

	/**
	 * Get discovered URIs for a given service name
	 * @param serviceName - name of the service
	 * @param minReplies - minimum number of requested URIs. Blocks until the number is satisfied.
	 * @return array with the discovered URIs for the given service name.
	 */
	public URI[] knownUrisOf(String serviceName, int minReplies);

	/**
	 * Get the instance of the Discovery service
	 * @return the singleton instance of the Discovery service
	 */
	public static Discovery getInstance() {
		return DiscoveryImpl.getInstance();
	}
}

/**
 * Implementation of the multicast discovery service
 */
class DiscoveryImpl implements Discovery {
	
	private static Logger Log = Logger.getLogger(Discovery.class.getName());

	 static {
	  	Log.setLevel(Level.WARNING);
	 }

	// The pre-aggreed multicast endpoint assigned to perform discovery.
	static final int DISCOVERY_RETRY_TIMEOUT = 5000;
	static final int DISCOVERY_ANNOUNCE_PERIOD = 1000;
	static final int RECORD_TTL = 2 * DISCOVERY_ANNOUNCE_PERIOD;
	static final int DISCOVERY_PORT = 2266;

	// Replace with appropriate values...
	// multicast IP range: 224.0.0.0 to 239.255.255.255
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", DISCOVERY_PORT);

	// Used separate the two fields that make up a service announcement.
	private static final String DELIMITER = "\t";

	private static final int MAX_DATAGRAM_SIZE = 65536;
	private static final URI[] NO_URIS = new URI[0];

	private static Discovery singleton;
	private final Map<String, Map<URI, Long>> records;

	synchronized static Discovery getInstance() {
		if (singleton == null) {
			singleton = new DiscoveryImpl();
		}
		return singleton;
	}

	private DiscoveryImpl() {
		this.startListener();
		this.records = new HashMap<>();
	}

	@Override
	public void announce(String serviceName, String serviceURI) {
		Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s\n", DISCOVERY_ADDR, serviceName,
				serviceURI));

		var pktBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();
		var pkt = new DatagramPacket(pktBytes, pktBytes.length, DISCOVERY_ADDR);

		// start thread to send periodic announcements
		new Thread(() -> {
			try (var ds = new DatagramSocket()) {
				while (true) {
					try {
						ds.send(pkt);
						Thread.sleep(DISCOVERY_ANNOUNCE_PERIOD);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}


	@Override
	public URI[] knownUrisOf(String serviceName, int minEntries) {
		var res = this.getRecords(serviceName);
		Log.info("Getting URI of: " + serviceName);
		var foundCount= res.isEmpty() ? 0 : res.get().length;
		if(foundCount < minEntries){
			Log.info(String.format("Ops just found %d records. I will wait a bit", foundCount));
			try{
				Thread.sleep(DISCOVERY_RETRY_TIMEOUT);
			} catch (InterruptedException e) {
				Log.info("Thread woke-up do what :)");
				e.printStackTrace();
			}
			res = this.getRecords(serviceName);
		}
		return res.orElse(NO_URIS);
	}

	private synchronized Optional<URI[]> getRecords(String serviceName){
		var values = this.records.get(serviceName);
		URI[] records;
		return values == null || (records = makeRecords(values)).length == 0
				? Optional.empty() : Optional.of(records);
	}

	private URI[] makeRecords(Map<URI, Long> recs){
		recs.values().removeIf(ctime -> System.currentTimeMillis() - ctime > RECORD_TTL);
		return recs.keySet().toArray(URI[]::new);
	}

	private void startListener() {
		Log.info(String.format("Starting discovery on multicast group: %s, port: %d\n", DISCOVERY_ADDR.getAddress(),
				DISCOVERY_ADDR.getPort()));

		new Thread(() -> {
			try (var ms = new MulticastSocket(DISCOVERY_ADDR.getPort())) {
				ms.joinGroup(DISCOVERY_ADDR, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
				for (;;) {
					try {
						var pkt = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE], MAX_DATAGRAM_SIZE);
						ms.receive(pkt);

						var msg = new String(pkt.getData(), 0, pkt.getLength());
						Log.info(String.format("Received: %s", msg));

						var parts = msg.split(DELIMITER);
						if (parts.length == 2) {
							var serviceName = parts[0];
							var uri = URI.create(parts[1]);
							synchronized (this){
								var record_values = records.computeIfAbsent(serviceName, k -> new HashMap<>());
								record_values.put(uri, System.currentTimeMillis());
								// record_values.add(new Record( // TODO: think about this later :)
								// 		System.currentTimeMillis(), uri
								// ));
							}
						}

					} catch (Exception x) {
						x.printStackTrace();
					}
				}
			} catch (Exception x) {
				x.printStackTrace();
			}
		}).start();
	}

	private record Record(long creationTime, URI value)implements Comparable<Record>{
		@Override
		public int compareTo(Record o) {
			return this.value.compareTo(o.value());
		}

		@Override
		public boolean equals(Object other){
			return other instanceof Record
					&& this.value.equals(((Record) other).value);
		}

		@Override
		public int hashCode() {
			return this.value.hashCode();
		}
	}

}