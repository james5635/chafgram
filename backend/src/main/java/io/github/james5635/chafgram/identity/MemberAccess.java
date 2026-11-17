package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Shared security-context helpers: resolving the current member and enforcing
 * that suspended/deactivated accounts cannot perform write operations
 * (functional-requirements.md: FR-1.4 — suspended members can read but not write).
 */
@Service
public class MemberAccess {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MemberAccess.class);

	private final UserRepository users;
	private final LastSeenService lastSeen;

	public MemberAccess(UserRepository users, LastSeenService lastSeen) {
		this.users = users;
		this.lastSeen = lastSeen;
	}

	public User currentMember(String username) {
		User user = users.findByUsername(username)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
		// FR-2.5: throttled presence; a last-seen failure must never break the request
		try {
			lastSeen.touch(user.getId());
		}
		catch (RuntimeException e) {
			log.debug("last-seen update failed for user {}: {}", user.getId(), e.getMessage());
		}
		return user;
	}

	public Optional<User> find(String username) {
		return users.findByUsername(username);
	}

	/** Write operations require an active (not suspended/deactivated) account. */
	public User requireWriteable(String username) {
		User user = currentMember(username);
		requireWriteable(user);
		return user;
	}

	/** Admin operations: active admin only. */
	public User requireAdmin(String username) {
		User user = requireWriteable(username);
		if (user.getRole() != Role.ADMIN) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Requires admin");
		}
		return user;
	}

	/** Staff operations (moderation + admin): active moderator or admin. */
	public User requireStaff(String username) {
		User user = requireWriteable(username);
		if (user.getRole() != Role.ADMIN && user.getRole() != Role.MODERATOR) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Requires moderator or admin");
		}
		return user;
	}

	public void requireWriteable(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN,
					"Account is " + user.getStatus().name().toLowerCase() + "; write operations are not allowed");
		}
	}
}
