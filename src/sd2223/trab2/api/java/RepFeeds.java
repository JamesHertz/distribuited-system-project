package sd2223.trab2.api.java;

import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.servers.java.JavaFeeds;
import sd2223.trab2.utils.IDGenerator;

public interface RepFeeds extends Feeds {
    IDGenerator getGenerator();
}
