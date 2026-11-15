package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Shared security-context helpers: resolving the current member and enforcing
 * that suspended/deactivated accounts cannot perform write operations
 * (plans.md: FR-1.4 — suspended members can read but not write).
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

	public void requireWriteable(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN,
					"Account is " + user.getStatus().name().toLowerCase() + "; write operations are not allowed");
		}
	}
}
