package io.github.james5635.chafgram.social;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the authenticated member from the security context. */
@Component
public class CurrentMember {

	private final UserRepository users;

	public CurrentMember(UserRepository users) {
		this.users = users;
	}

	public User require() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null
				|| auth instanceof AnonymousAuthenticationToken
				|| "anonymousUser".equals(auth.getPrincipal())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
		}
		return users.findByUsername(auth.getName())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
	}
}
