package sd2223.trab1.servers.java;


import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.clients.ClientFactory;
import sd2223.trab1.discovery.Discovery;
import sd2223.trab1.utils.Formatter;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaService {

    private final Map<String, RequestConsumer> consumers;

    public JavaService() {
        this.consumers = new HashMap<>();
    }

    public <T> void addRequest(String domain, Request<T> request) {
        this.addRequest(domain, request, false);
    }

    public <T> void addRequest(String domain, Request<T> request, boolean forceBackground) {
        RequestConsumer aux;
        synchronized (consumers) {
            aux = consumers.computeIfAbsent(domain, k -> {
                return new RequestConsumer(this.getFeedServer(domain));
            });
        }
        aux.addRequest(request, forceBackground);
    }

    protected Feeds getFeedServer(String serverDomain) {
        var ds = Discovery.getInstance();
        URI[] serverURI = ds.knownUrisOf(Formatter.getServiceID(serverDomain, Formatter.FEEDS_SERVICE), 1);
        if (serverURI.length == 0) return null;
        return ClientFactory.getFeedsClient(serverURI[0]);
    }

    @FunctionalInterface
    interface Request<T> {
        Result<T> execute(Feeds server);
    }

    private static class RequestConsumer {
        private static final long WAIT_TIME = 2000;
        private final Queue<Request<?>> requestQueue;
        private final Queue<Request<?>> newRequests;
        private final Feeds remoteServer;
        // private final AtomicBoolean waitingForMessages;

        public RequestConsumer(Feeds remoteServer) {
            this.remoteServer = remoteServer;
            this.requestQueue = new LinkedList<>();
            this.newRequests = new LinkedList<>();
            // this.waitingForMessages = new AtomicBoolean(false);
            new Thread(
                    this::loop
            ).start();
        }

        public void addRequest(Request<?> request, boolean forceBackground) {
            boolean probablyWaiting = this.requestQueueIsEmpty();
            synchronized (newRequests) {
                // if the queue is empty try to send the request if it fails add it to the queue
                // newRequests.add(request);
                // if(probablyWaiting)
                //     newRequests.notifyAll();
                 if (forceBackground || !(probablyWaiting && newRequests.isEmpty()
                         && this.executeRequest(request))) {
                     System.out.println("Adding message to the queue...");
                     if (probablyWaiting)
                         newRequests.notifyAll();
                 }
            }

        }

        public boolean requestQueueIsEmpty() {
            synchronized (requestQueue) {
                return requestQueue.isEmpty();
            }
        }

        private boolean executeRequest(Request<?> req) {
            synchronized (remoteServer) {
                var res = req.execute(remoteServer);
                return res.isOK() || res.error() != Result.ErrorCode.TIMEOUT; // request failed
            }
        }

        private void loop() {
            for (;;) {
                if (this.requestQueueIsEmpty()) this.getNewRequest();
                Request<?> req;
                synchronized (requestQueue) {
                    req = requestQueue.peek();
                }

                var success = this.executeRequest(req);
                if (! success ) {
                    this.sleep(WAIT_TIME); // wait a bit before retrying :)
                    continue; // retry
                }

                synchronized (requestQueue) {
                    requestQueue.remove();
                }
            }
        }

        private void getNewRequest() {
            synchronized (newRequests) {
                while (newRequests.isEmpty()) {
                    try {
                        newRequests.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                synchronized (requestQueue) {
                    requestQueue.addAll(newRequests);
                }
                newRequests.clear();
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