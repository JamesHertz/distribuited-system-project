package sd2223.trab2.api;

public class Update {
    private int operation;
    private String[] args;

    public Update(){
        this(0, null);
    }
    public Update(int operation, String[] args) {
        this.operation = operation;
        this.args = args;
    }

    public int getOperation() {
        return operation;
    }

    public String[] getArgs() {
        return args;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public static Update toUpdate(Operations op, Object ...args){
        var upArgs = new String[args.length];
        for(int i = 0; i < args.length; i++) {
            upArgs[i] = args[i].toString();
        }
        return new Update(op.operationID(), upArgs);
    }
}
