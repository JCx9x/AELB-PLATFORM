package es.aelb.backendweb.infrastructure.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limitador de ventana fija por clave arbitraria (IP, IP+email, IP+ruta...).
 * En memoria del propio proceso: válido mientras el backend corra en una
 * única instancia. Si en el futuro se despliega con varias réplicas detrás
 * de un balanceador, este contador deja de ser global y hay que moverlo a
 * un almacén compartido (p. ej. Redis/ElastiCache) para que el límite se
 * aplique de verdad entre instancias.
 */
@Component
public class InMemoryRateLimiter {

    private static final Duration MAX_ENTRY_AGE = Duration.ofMinutes(30);

    private record Window(AtomicLong windowStartMillis, AtomicInteger count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** Devuelve true si la petición identificada por {@code key} entra dentro del límite. */
    public boolean tryConsume(String key, int limit, Duration window) {
        long windowMillis = window.toMillis();
        long now = System.currentTimeMillis();

        Window bucket = windows.computeIfAbsent(key, k -> new Window(new AtomicLong(now), new AtomicInteger(0)));

        synchronized (bucket) {
            if (now - bucket.windowStartMillis().get() >= windowMillis) {
                bucket.windowStartMillis().set(now);
                bucket.count().set(0);
            }
            return bucket.count().incrementAndGet() <= limit;
        }
    }

    @Scheduled(fixedDelay = 10, initialDelay = 10, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    void evictStaleEntries() {
        long cutoff = System.currentTimeMillis() - MAX_ENTRY_AGE.toMillis();
        windows.entrySet().removeIf(entry -> entry.getValue().windowStartMillis().get() < cutoff);
    }
}
