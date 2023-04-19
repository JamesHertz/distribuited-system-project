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
    private static final long DEFAULT_TIMEOUT = 2000;
    private final Queue<Request<Feeds>> newRequests;

    public JavaService() {
        this.newRequests = new LinkedList<>();
        new Thread(this::run).start();
    }

    private void run() {
        Queue<Request<Feeds>> requests = new LinkedList<>();
        Map<String, Feeds> clients     = new HashMap<>();
        for (;;) {
            synchronized (newRequests) {
                requests.addAll(newRequests);
                newRequests.clear();
                try {
                    if (requests.isEmpty()) {
                        newRequests.wait();
                    }
                } catch (InterruptedException e) {
                    continue;
                }
            }
            int times = requests.size();
            for(int i = 0; i < times; ++i){
                var req = requests.remove();
                var client = clients.computeIfAbsent(req.serviceID(), this::getFeedServer);
                var res = req.performAction(client);
                if (!res.isOK() || res.error() == Result.ErrorCode.TIMEOUT) {
                   requests.add(req);
                }
                /*
                    else {
                        // clients.remove(req.serviceID());   // todo: think about this
                    }
                */
                /*
                      fct di
                      failures = { fct }
                      requests = {
                            (di, ...)
                            (fct, ...) 13s
                            (fct, ...) 13s
                            (fct, ...) 13s
                      }
                 */
            }
            this.sleep(DEFAULT_TIMEOUT);
        }
    }

    public void addRequest(String domain, RequestAction<Feeds> action){
       synchronized (newRequests){
           newRequests.add(new Request<>(domain, action));
           newRequests.notifyAll();
       }
    }

    protected Feeds getFeedServer(String serverDomain) {
        var ds = Discovery.getInstance();
        URI[] serverURI = ds.knownUrisOf(Formatter.getServiceID(serverDomain, Formatter.FEEDS_SERVICE), 1);
        if (serverURI.length == 0) return null;
        return ClientFactory.getFeedsClient(serverURI[0]);
    }
    private void sleep(long ms){
        try{
            Thread.sleep(ms);
        } catch (InterruptedException e) {
           // ola
        }

    }

    record Request<T>(String serviceID, RequestAction<T> action) {
        Result<?> performAction(T client) {
            return action.performAction(client);
        }
    }

    @FunctionalInterface
    interface RequestAction<T> {
        Result<?> performAction(T client);
    }


}
