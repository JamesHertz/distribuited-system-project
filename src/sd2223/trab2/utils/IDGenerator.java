package sd2223.trab2.utils;

import java.util.Random;

/**
 *  Used to generate the messages ID
 */
public class IDGenerator {

    private static final long MAGIC_NUMBER = 256L;
    private final long baseNumber;
    private long seqNumber;

    public IDGenerator(long baseNumber) {
        this.baseNumber = baseNumber;
        this.seqNumber  = new Random().nextLong(0, 10000);
    }

    public synchronized long nextID(){
        return ++seqNumber * MAGIC_NUMBER + baseNumber;
    }
}
