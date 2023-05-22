package sd2223.trab2.utils;

public class Secret {
    private static String secret;
    public static void setSecret(String secret){
        Secret.secret = secret;  // do this once :)
    }

    public static synchronized String getSecret(){
        return secret;
    }
}
