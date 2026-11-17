package io.github.doguomer.esign.api;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bellek içi, süreli ve sınırlı oturum deposu. Veritabanı gerekmez; sunucu yeniden başlarsa yarım kalan
 * imzalar yeniden hazırlanır. Kimlikler 128 bit rastgeledir (tahmin edilemez).
 * In-memory, bounded, expiring session store with unguessable 128-bit ids.
 */
final class SessionStore<T> {

    private record Entry<T>(T value, Instant expiresAt) {}

    private final Map<String, Entry<T>> map = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final Duration ttl;
    private final int max;

    SessionStore(Clock clock, Duration ttl, int max) {
        this.clock = Objects.requireNonNull(clock);
        this.ttl = Objects.requireNonNull(ttl);
        if (ttl.isNegative() || ttl.isZero() || max < 1) {
            throw new IllegalArgumentException("Geçersiz oturum ayarı");
        }
        this.max = max;
    }

    synchronized String put(T value) {
        purge();
        if (map.size() >= max) {
            throw new TooManySessionsException(max);
        }
        byte[] b = new byte[16];
        random.nextBytes(b);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        map.put(id, new Entry<>(value, clock.instant().plus(ttl)));
        return id;
    }

    /** Oturumu döndürür ve siler (tek kullanımlık). */
    Optional<T> take(String id) {
        if (id == null) {
            return Optional.empty();
        }
        Entry<T> e = map.remove(id);
        return e == null || !clock.instant().isBefore(e.expiresAt()) ? Optional.empty() : Optional.of(e.value());
    }

    Optional<Instant> expiresAt(String id) {
        return Optional.ofNullable(map.get(id)).map(Entry::expiresAt);
    }

    int size() {
        purge();
        return map.size();
    }

    private void purge() {
        Instant now = clock.instant();
        map.values().removeIf(e -> !now.isBefore(e.expiresAt()));
    }

    /** Eşzamanlı oturum sınırı aşıldı. */
    static final class TooManySessionsException extends RuntimeException {
        TooManySessionsException(int max) {
            super("Çok fazla bekleyen imza oturumu (" + max + "); daha sonra tekrar deneyin");
        }
    }
}
