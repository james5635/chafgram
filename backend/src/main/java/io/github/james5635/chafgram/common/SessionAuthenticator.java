package io.github.james5635.chafgram.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Programmatically establishes a session-authenticated context for a member
 * (used by bootstrap / invite acceptance / open registration, where creating
 * the account happens outside the usual login flow).
 */
@Component
public class SessionAuthenticator {

	private final SecurityContextRepository securityContextRepository;

	public SessionAuthenticator(SecurityContextRepository securityContextRepository) {
		this.securityContextRepository = securityContextRepository;
	}

	public void authenticate(String username, List<SimpleGrantedAuthority> authorities,
			HttpServletRequest request, HttpServletResponse response) {
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(username, null, authorities);
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		securityContextRepository.saveContext(context, request, response);
		SecurityContextHolder.setContext(context);
	}
}
