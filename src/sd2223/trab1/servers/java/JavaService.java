package sd2223.trab1.servers.java;

import sd2223.trab1.api.java.Result;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

public class JavaService<T> {
    private final Queue<Request<T>> requests;
    private final ClientBuilder<T> serviceBuilder;

    public JavaService(ClientBuilder<T> serviceBuilder) {
        this.serviceBuilder = serviceBuilder;
        this.requests = new LinkedList<>();
        new Thread(this::run).start();
    }

    private void run() {
        // map with all the clients
        for (;;) {
            Request<T> req;
            synchronized (requests) {
                try {
                    if (requests.isEmpty()) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    continue;
                }
                req = requests.remove();
            }

            var client = serviceBuilder.build(req.serviceID());
            var res = req.performAction(client);
            if (!res.isOK() || res.error() == Result.ErrorCode.TIMEOUT) {
                requests.add(req);
            }
            // wait :) few seconds
        }
    }

    public void addRequest(String serviceID, RequestAction<T> action){
       synchronized (requests){
           requests.add(new Request<>(serviceID, action));
           if(requests.size() == 1){ // the one I just added
               requests.notifyAll();
           }
       }
    }

    record Request<T>(String serviceID, RequestAction<T> action) {
        Result<?> performAction(T client) {
            return action.performAction(client);
        }
    }

    ;

    interface ClientBuilder<T> {
        T build(String serviceID);
    }

    interface RequestAction<T> {
        Result<?> performAction(T client);
    }


}
