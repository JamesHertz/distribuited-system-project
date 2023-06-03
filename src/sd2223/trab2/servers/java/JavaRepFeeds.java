package sd2223.trab2.servers.java;

import sd2223.trab2.api.Message;
import sd2223.trab2.api.java.RepFeeds;
import sd2223.trab2.utils.IDGenerator;

public class JavaRepFeeds extends JavaFeeds implements RepFeeds {
    public JavaRepFeeds(String domain, long baseNumber) {
        super(domain, baseNumber);
    }

    @Override
    protected void setupMessage(Message msg) {
        // do nothing  :) ( because the primary will do that for us :) )
    }


    @Override
    public IDGenerator getGenerator() {
        return super.generator;
    }
}

