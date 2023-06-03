package sd2223.trab2.servers.java;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.clients.ClientFactory;
import sd2223.trab2.discovery.Discovery;
import sd2223.trab2.utils.Formatter;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class JavaService {
    private static Logger Log = LoggerFactory.getLogger(JavaUsers.class.getName());

    private final Map<String, RequestConsumer> consumers;

    public JavaService() {
        this.consumers = new HashMap<>();
    }

    public <T> void addRequest(String domain, Request<T> request) {
        this.addRequest(domain, request, false);
    }

    /**
     * Adds a RequestConsumer to our map (If it isn't there already), with the domain's feedserver.
     * Then adds the actual request to the RequestConsumer
     * @param domain domain of the feeds server
     * @param request the request to be executed
     * @param forceBackground to force the request to be executed in the background
     *                        (otherwise it would do a test to decide if it would or not be executed in such a way)
     * @param <T> whatever the request returns
     */
    public <T> void addRequest(String domain, Request<T> request, boolean forceBackground) {
        Log.debug("New request: domain = {}", domain);
        RequestConsumer aux;
        synchronized (consumers) {
            aux = consumers.computeIfAbsent(domain, k -> {
                return new RequestConsumer(this.getFeedServer(domain));
            });
        }
        // Adds the actual request to the Request consumer
        aux.addRequest(request, forceBackground);
    }

    protected Feeds getFeedServer(String serverDomain) {
        var ds = Discovery.getInstance();
        URI[] serverURI = ds.knownUrisOf(Formatter.getServiceID(serverDomain, Formatter.FEEDS_SERVICE), 1);
        if (serverURI.length == 0) return null;
        return ClientFactory.getFeedsClient(serverURI[0]);
    }

    // represents a request to a feed server
    @FunctionalInterface
    interface Request<T> {
        Result<T> execute(Feeds server);
    }

    /**
     * This class represents a server who on which we will make a list of requests (In short terms, we have a list of requests for each server separately)
     */
    private static class RequestConsumer {
        private static final long WAIT_TIME = 2000;
        //Queue where requests will be executed
        private final Queue<Request<?>> requestQueue;

        //Acts like a waiting room for the requests
        private final Queue<Request<?>> newRequests;
        private final Feeds remoteServer;


        /**
         * When class is created, starts the loop function here as a thread
         * @param remoteServer
         */
        public RequestConsumer(Feeds remoteServer) {
            this.remoteServer = remoteServer;
            this.requestQueue = new LinkedList<>();
            this.newRequests = new LinkedList<>();
            // launches a background thread
            new Thread(
                    this::loop
            ).start();
        }

        /**
         * Adds the request to this specific RequestConsumer
         * @param request
         * @param forceBackground  true if the request is supposed to be put in the requestQueue (and not perform a test in the foreground to such decision)
         */
        public void addRequest(Request<?> request, boolean forceBackground) {
            boolean probablyWaiting = this.requestQueueIsEmpty(); //If the queue of requests is empty, means it is probablyWaiting for new requests
            synchronized (newRequests) {
                // if the queue is empty try to send the request if it fails add it to the queue
                 if (    forceBackground //If we force the request to go, it goes
                         || !( probablyWaiting && newRequests.isEmpty()
                         && this.executeRequest(request) ) //Tries to execute, if it fails with timeout, means that there was some connection error, so add to queue to try again later
                 ) {
                     newRequests.add(request);
                     Log.debug("Request added to newRequest list");
                     if (probablyWaiting)
                         newRequests.notifyAll();
                 } else
                    Log.debug("Request performed in foreground and succeeded!");

            }

        }

        public boolean requestQueueIsEmpty() {
            synchronized (requestQueue) {
                return requestQueue.isEmpty();
            }
        }

        /**
         * Auxiliary method to see if the request can execute (Don't give timeout responses)
         * @param req request to be executed
         * @return true if executes (request was receive and answer by the other side), otherwise false
         */
        private boolean executeRequest(Request<?> req) {
            synchronized (remoteServer) {
                var res = req.execute(remoteServer);
                Log.debug("+1 execution (res={})", res);
                return res.isOK() || res.error() != Result.ErrorCode.TIMEOUT; // was not processed by the remote server
            }
        }

        /**
         * Loop function to get newRequests, and then trying to execute them
         */
        private void loop() {
            for (;;) {
                //If there is no requests, try and get one
                if (this.requestQueueIsEmpty()) this.getNewRequest();
                Request<?> req;
                synchronized (requestQueue) {
                    req = requestQueue.peek(); // get the request in the front of the queue
                }

                var success = this.executeRequest(req);
                if (! success ) {
                    this.sleep(WAIT_TIME); // wait a bit before retrying :)
                    continue; // retry
                }

                synchronized (requestQueue) {
                    requestQueue.remove();
                    Log.debug("loop: send +1 message  - remaining=" + requestQueue.size());
                }
            }
        }

        /**
         * Waits until it gets a new request
         */
        private void getNewRequest() {
            synchronized (newRequests) {
                while (newRequests.isEmpty()) { // While the newRequests queue is empty
                    try {
                        newRequests.wait(); // Waits... Until it is notified by the addRequest method here
                    } catch (InterruptedException ignored) {
                    }
                }
                synchronized (requestQueue) {
                    requestQueue.addAll(newRequests); // Adds all newRequests to the requestQueue
                    Log.info("getNewMessages: got " + newRequests.size() + " messages.");
                }
                newRequests.clear(); // Then clear the newRequests
            }
        }

        private void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                // do nothing
            }

        }
    }


}