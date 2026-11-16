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

	private final UserRepository users;

	public MemberAccess(UserRepository users) {
		this.users = users;
	}

	public User currentMember(String username) {
		return users.findByUsername(username)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
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
