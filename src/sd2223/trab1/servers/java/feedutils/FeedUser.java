package sd2223.trab1.servers.java.feedutils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class FeedUser {
    private final Feed personalFeed;
    private final Set<String> subscriptions; // users
    private final Set<String> subscribers; // server subscribed at this user :)

    public FeedUser(){
       this.personalFeed = new Feed();
       this.subscriptions = new HashSet<>();
       this.subscribers = new HashSet<>();
    }

    public Feed getPersonalFeed(){
       return personalFeed;
    }

    public void addSubscription(String userAddress){
        subscriptions.add(userAddress); // inscriptions time
    }

    public void removeSubscription(String userAddress){
        subscriptions.remove(userAddress);
    }

    public void addSubscriber(String serverID){
        subscribers.add(serverID);
    }

    public void removeSubscriber(String serverID){
        subscribers.remove(serverID);
    }

    public Stream<String> getAllSubscriptions(){
       return subscriptions.stream();
    }

    public Stream<String> getAllSubscribers(){
        return subscribers.stream();
    }

}
