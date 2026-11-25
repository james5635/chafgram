package io.github.james5635.chafgram.identity.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.UserStatus;

/**
 * Loads members for Spring Security (form login and as the principal source
 * for the WebAuthn ceremonies).
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
		var member = users.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Unknown username: " + username));

		if (member.getStatus() == UserStatus.DEACTIVATED) {
			throw new UsernameNotFoundException("Account is deactivated: " + username);
		}

		List<SimpleGrantedAuthority> authorities =
				List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));

		// Accounts without a password are passkey-only; form login stays possible
		// only for accounts that set one (Spring rejects null passwords, so use a
		// deliberately unmatchable marker).
		String password = member.getPasswordHash() != null
				? member.getPasswordHash()
				: "{invalid}passkey-only-account";

		return new User(member.getUsername(), password, true, true, true, true, authorities);
	}
}
