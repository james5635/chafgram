package io.github.james5635.chafgram.identity.service;

import io.github.james5635.chafgram.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserStatus;

/**
 * Shared security-context helpers: resolving the current member and enforcing
 * that suspended/deactivated accounts cannot perform write operations
 * (functional-requirements.md: FR-1.4 — suspended members can read but not write).
 */
@Service
public class MemberAccess {

	private static final Logger log = LoggerFactory.getLogger(MemberAccess.class);

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
		touchPresence(user);
		return user;
	}

	/**
	 * Records the activity of the given member. The guard covers the whole
	 * transactional call — a presence write that only fails at commit must not
	 * take the request down with it.
	 */
	private void touchPresence(User user) {
		try {
			lastSeen.touch(user.getId());
		}
		catch (RuntimeException e) {
			log.debug("last-seen update failed for user {}: {}", user.getId(), e.getMessage());
		}
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
