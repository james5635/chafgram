package io.github.james5635.chafgram.identity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Throttled last-seen presence (FR-2.5): at most one write per member per five
 * minutes, so feed/profile traffic does not turn into a write storm.
 */
@Service
public class LastSeenService {

	/** Minimum gap between two last-seen writes for the same member. */
	static final Duration THROTTLE = Duration.ofMinutes(5);

	private final UserRepository users;

	public LastSeenService(UserRepository users) {
		this.users = users;
	}

	/**
	 * Records the member's activity.
	 * <p>
	 * The write runs in its own transaction, so it never joins a read-only
	 * request transaction. Presence is best-effort: callers must tolerate a
	 * failure and must not let it break the request that triggered it. The
	 * failure can surface at commit time (a rollback-only transaction), so the
	 * caller has to guard the whole call, not just the update.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void touch(Long userId) {
		Instant now = Instant.now();
		users.touchLastSeen(userId, now, now.minus(THROTTLE));
	}
}
