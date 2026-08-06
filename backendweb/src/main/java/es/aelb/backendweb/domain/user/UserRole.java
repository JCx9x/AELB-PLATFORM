package es.aelb.backendweb.domain.user;

public enum UserRole {
    USER,
    GESTOR,
    ADMIN;

    public boolean canManageChampionships() {
        return this == GESTOR || this == ADMIN;
    }

    public boolean canManageUsers() {
        return this == ADMIN;
    }

    public boolean canManageNews() {
        return this == GESTOR || this == ADMIN;
    }
}
