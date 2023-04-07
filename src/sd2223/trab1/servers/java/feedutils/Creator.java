package sd2223.trab1.servers.java.feedutils;

public class Creator {
    private Feed feed;
    private int subsCount;
    public Creator(){
        this.feed = new Feed();
        this.subsCount = 0;
    }

    public Feed getFeed(){
        return this.feed;
    }

    public int getSubsCount(){
        return this.subsCount;
    }

    public void addSubscription(){
       this.subsCount += 1;
    }

    public void removeSubscription(){
        this.subsCount -= 1;
    }
}
