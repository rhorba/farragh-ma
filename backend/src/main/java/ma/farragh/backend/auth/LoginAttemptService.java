package ma.farragh.backend.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory lockout tracker (pilot scale, single instance — see System Design doc).
 * 5 failed attempts locks the account for 15 minutes, per Security Baseline doc.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 15 * 60 * 1000;

    private record Attempts(int count, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    public boolean isLocked(String email) {
        Attempts attempts = attemptsByEmail.get(email);
        return attempts != null && attempts.lockedUntil() != null && Instant.now().isBefore(attempts.lockedUntil());
    }

    public void recordFailure(String email) {
        attemptsByEmail.compute(email, (key, existing) -> {
            int count = (existing == null ? 0 : existing.count()) + 1;
            Instant lockedUntil = count >= MAX_ATTEMPTS ? Instant.now().plusMillis(LOCKOUT_MS) : null;
            return new Attempts(count, lockedUntil);
        });
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(email);
    }
}
