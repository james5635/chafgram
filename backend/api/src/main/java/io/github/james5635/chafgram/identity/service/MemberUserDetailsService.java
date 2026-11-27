package io.github.james5635.chafgram.identity.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.UserStatus;

/**
 * Loads members for Spring Security's HTTP Basic actuator chain and password
 * credential checks.
 *
 * <p>Deactivated accounts cannot authenticate at all (FR-1.4). Suspended
 * accounts may sign in — enforcement that suspended members cannot perform
 * write operations lives in the service layer (see {@link MemberAccess}).</p>
 */
@Service
public class MemberUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public MemberUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		String login = username == null ? "" : username.trim();
		String normalized = login.toLowerCase(Locale.ROOT);
		var member = users.findByUsernameOrEmail(normalized, normalized)
				.orElseThrow(() -> new UsernameNotFoundException("Unknown username: " + login));

		if (member.getStatus() == UserStatus.DEACTIVATED) {
			throw new UsernameNotFoundException("Account is deactivated: " + username);
		}

		List<SimpleGrantedAuthority> authorities =
				List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));

		// Spring rejects null passwords, so accounts without a password get a
		// deliberately unmatchable marker instead.
		String password = member.getPasswordHash() != null
				? member.getPasswordHash()
				: "{invalid}no-password-account";

		return new User(member.getUsername(), password, true, true, true, true, authorities);
	}
}
