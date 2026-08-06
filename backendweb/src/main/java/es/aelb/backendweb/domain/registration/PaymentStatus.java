package es.aelb.backendweb.domain.registration;

public enum PaymentStatus {
    PENDING,
    PAID,
    CANCELLED;

    public boolean isActive() {
        return this != CANCELLED;
    }
}
