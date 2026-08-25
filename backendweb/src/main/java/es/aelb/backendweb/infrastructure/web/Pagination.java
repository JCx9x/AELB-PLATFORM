package es.aelb.backendweb.infrastructure.web;

/** Shared API limits: zero-based pages and a bounded page size. */
public record Pagination(int page, int size) {
    public static Pagination of(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page debe ser mayor o igual que 0");
        return new Pagination(page, Math.max(1, Math.min(size, 100)));
    }
}
