package sd2223.trab1.servers.java.feedutils;

import sd2223.trab1.api.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Feed {
   // owner

    private final Map<Long, Message> messages;
    public Feed(){
       messages = new HashMap<>();
    }

    public Message getMessage(long mid){
       return messages.get(mid);
    }
    public void addMessage(long mid, Message msg){
        messages.put(mid, msg);
    }

    public  boolean removeMessage(long mid){
        return messages.remove(mid) != null;
    }

    public Stream<Message> getAllMessages(){
        return messages.values().stream();
    }

}
