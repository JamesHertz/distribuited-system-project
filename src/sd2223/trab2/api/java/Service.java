package sd2223.trab2.api.java;

public interface Service  {
    enum ServiceType {
        FEEDS, USERS, PROXY, REPLICATION;
    }
    enum Protocol {
        REST, SOAP, REPLICATION;
    }
}
