package io.github.james5635.chafgram.social.utils;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.service.LastSeenService;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the authenticated member from the security context. */
@Component
public class CurrentMember {

	private static final Logger log = LoggerFactory.getLogger(CurrentMember.class);

	private final UserRepository users;
	private final LastSeenService lastSeen;

	public CurrentMember(UserRepository users, LastSeenService lastSeen) {
		this.users = users;
		this.lastSeen = lastSeen;
	}

	public User require() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null
				|| auth instanceof AnonymousAuthenticationToken
				|| "anonymousUser".equals(auth.getPrincipal())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
		}
		User user = users.findByUsername(auth.getName())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
		// FR-2.5: reading the API is activity — refresh the throttled presence
		touchPresence(user);
		return user;
	}

	/**
	 * Records the activity of the given member. The guard covers the whole
	 * transactional call — a presence write that only fails at commit must not
	 * take the read down with it.
	 */
	private void touchPresence(User user) {
		try {
			lastSeen.touch(user.getId());
		}
		catch (RuntimeException e) {
			log.debug("last-seen update failed for user {}: {}", user.getId(), e.getMessage());
		}
	}
}
