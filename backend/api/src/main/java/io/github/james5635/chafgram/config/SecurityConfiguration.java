package io.github.james5635.chafgram.config;

import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Authentication: short-lived bearer JWTs on the main chain (no sessions, no
 * CSRF), HTTP Basic on the actuator chain. Authorization is enforced in the
 * `authorizeHttpRequests` matrix (coarse), method security (moderator/admin
 * endpoints), and the service layer (suspended accounts cannot write).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

	private final UserRepository users;

	public SecurityConfiguration(UserRepository users) {
		this.users = users;
	}

	/**
	 * Actuator endpoints get their own chain (FR-10.4).
	 *
	 * <p>Health is a public liveness probe; {@code info} and {@code metrics}
	 * are admin-only. HTTP Basic is the only entry point here, so an
	 * anonymous request gets a {@code 401} with a {@code WWW-Authenticate}
	 * challenge, never a bearer-token error or an HTML login redirect.</p>
	 */
	@Bean
	@Order(1)
	SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
		http
			.securityMatcher("/actuator/**")
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				.anyRequest().hasRole("ADMIN"))
			.httpBasic(Customizer.withDefaults());
		return http.build();
	}

	/**
	 * Stateless resource-server chain. The bearer filter validates each access
	 * JWT (signature, expiry, issuer, audience) and populates the
	 * {@code SecurityContext} for the duration of the request only — no
	 * {@code JSESSIONID} or {@code XSRF-TOKEN} is ever created.
	 *
	 * <p>The HTTP boundary stays public for the documented onboarding/auth
	 * endpoints, the signed media bytes URL, and GraphQL (whose resolvers keep
	 * their own {@code @PreAuthorize} rules and HTTP 200 + {@code errors[]}
	 * failure shape). Everything else requires a bearer token.</p>
	 */
	@Bean
	@Order(2)
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/api/v1/workspace").permitAll()
				.requestMatchers("/api/v1/bootstrap", "/api/v1/register", "/api/v1/invites/accept")
					.permitAll()
				.requestMatchers("/api/v1/auth/**").permitAll()
				// Signed bytes links carry exp/sig in the query string; the
				// controller/service validate them (Task 4). Header-authenticated
				// callers still go through the same controller, which requires a member.
				.requestMatchers(HttpMethod.GET, "/api/v1/media/{id}/bytes").permitAll()
				.requestMatchers("/error").permitAll()
				// GraphQL and GraphiQL are public at the HTTP boundary; authorization
				// is enforced per resolver (class-level @PreAuthorize("isFullyAuthenticated()"),
				// with "permitAll()" on explicit public operations only).
				.requestMatchers("/graphql", "/graphiql").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2
				// The auth endpoints are public and validate their own credentials:
				// refresh is authenticated by the cookie, and logout deliberately
				// accepts an expired access JWT so a client idle past the short TTL
				// can still revoke its refresh token. A stale bearer forwarded by
				// the BFF must not be rejected by the strict resource-server filter
				// before those controllers run.
				.bearerTokenResolver(authEndpointBearerTokenResolver())
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return http.build();
	}

	private BearerTokenResolver authEndpointBearerTokenResolver() {
		BearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
		return request -> request.getRequestURI().startsWith("/api/v1/auth/")
				? null
				: defaultResolver.resolve(request);
	}

	/**
	 * The access JWT's {@code sub} is the member id, but resolvers and REST
	 * controllers identify the caller by username. Resolve it once in the
	 * converter so the existing {@code CurrentMember} / {@code MemberAccess}
	 * helpers keep working unchanged; fall back to the raw id if the member was
	 * deleted (the resolver's lookup then fails closed).
	 */
	private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
		return jwt -> {
			Collection<GrantedAuthority> authorities = jwt.getClaimAsStringList("roles") == null
					? List.of()
					: jwt.getClaimAsStringList("roles").stream()
						.map(role -> new SimpleGrantedAuthority(
								"ROLE_" + role.toUpperCase(Locale.ROOT)))
						.map(GrantedAuthority.class::cast)
						.toList();
			return new JwtAuthenticationToken(jwt, authorities, principalName(jwt));
		};
	}

	private String principalName(Jwt jwt) {
		String subject = jwt.getSubject();
		if (subject == null) {
			return "";
		}
		try {
			return users.findById(Long.valueOf(subject))
					.map(User::getUsername)
					.orElse(subject);
		}
		catch (NumberFormatException e) {
			return subject;
		}
	}

	/**
	 * The legacy {@code SessionAuthenticator} component still requires a
	 * {@link SecurityContextRepository}. Nothing calls it since onboarding
	 * switched to token pairs, and a null repository keeps it from creating a
	 * session if that ever changes. Remove it together with the component.
	 */
	@Bean
	SecurityContextRepository securityContextRepository() {
		return new NullSecurityContextRepository();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
