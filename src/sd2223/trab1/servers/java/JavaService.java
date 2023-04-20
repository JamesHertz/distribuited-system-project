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

public class JavaService {

    private final Map<String, RequestConsumer> consumers;
    public JavaService(){
        this.consumers = new HashMap<>();
    }

    public <T> void addRequest(String domain, Request<T> request){
        RequestConsumer aux;
        synchronized (consumers){
            aux = consumers.computeIfAbsent(domain, k -> {
                return new RequestConsumer( this.getFeedServer(domain) );
            });
        }
        aux.addRequest(request);
    }

    protected Feeds getFeedServer(String serverDomain) {
        var ds = Discovery.getInstance();
        URI[] serverURI = ds.knownUrisOf(Formatter.getServiceID(serverDomain, Formatter.FEEDS_SERVICE), 1);
        if (serverURI.length == 0) return null;
        return ClientFactory.getFeedsClient(serverURI[0]);
    }

    @FunctionalInterface
    interface Request <T> {
       Result<T> execute(Feeds server);
    }

    private static class RequestConsumer {
        private final Queue<Request<?>> requestQueue;
        private final Queue<Request<?>> newRequests;
        private final Feeds remoteServer;

        public RequestConsumer(Feeds remoteServer) {
            this.remoteServer = remoteServer;
            this.requestQueue = new LinkedList<>();
            this.newRequests = new LinkedList<>();
            new Thread(
                    this::loop
            ).start();
        }

        private void loop() {
            for (;;) {
                if(this.requestQueueIsEmpty()) this.getNewRequest();
                Request<?> req;
                synchronized (requestQueue){
                    req = requestQueue.peek();
                }

                assert req != null;
                var res = req.execute(remoteServer);
                if(!res.isOK() && res.error() == Result.ErrorCode.TIMEOUT) continue;

                synchronized (requestQueue){
                    requestQueue.remove();
                }
            }
        }

        private void getNewRequest(){
           synchronized (newRequests){
               while(newRequests.isEmpty()){
                   try{
                       newRequests.wait();
                   } catch (InterruptedException ignored) {}
               }
               synchronized (requestQueue){
                   requestQueue.addAll( newRequests );
               }
           }
        }
        public void addRequest(Request<?> request) {
            boolean probablyWaiting = this.requestQueueIsEmpty();
            synchronized (newRequests) {
                newRequests.add(request);
                if(probablyWaiting)
                    newRequests.notifyAll();
            }
        }

        public boolean requestQueueIsEmpty() {
            synchronized (requestQueue) {
                return requestQueue.isEmpty();
            }
        }
    }


}