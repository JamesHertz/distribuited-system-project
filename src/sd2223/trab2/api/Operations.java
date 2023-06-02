package sd2223.trab2.api;

public enum Operations {
    CREATE_MESSAGE, REMOVE_FROM_FEED, SUBSCRIBE_USER,
    UNSUBSCRIBE_USER, SUBSCRIBE_SERVER, UNSUBSCRIBE_SERVER,
    CREATE_FEED, CREATE_EXT_FEED, REMOVE_FED, REMOVE_EXT_FEED;

    private static final Operations[] ops = values();

    int operationID(){
        return this.ordinal();
    }

    public static Operations valueOf(int opID){
       return opID < 0 || opID >= ops.length ? null : ops[opID];
    }

}
