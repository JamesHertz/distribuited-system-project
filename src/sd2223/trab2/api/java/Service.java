package sd2223.trab2.api.java;

public interface Service  {
    enum ServiceType {
        FEEDS, USERS, PROXY;
    }
    enum Protocol {
        REST, SOAP, REPLICATION;
    }
}
