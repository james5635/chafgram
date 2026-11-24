package io.github.james5635.chafgram.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;

import java.util.List;

/**
 * Authentication: passkeys-first (Spring Security WebAuthn) plus optional
 * password login for members who set one. Authorization is enforced in the
 * `authorizeHttpRequests` matrix (coarse), method security (moderator/admin
 * endpoints), and the service layer (suspended accounts cannot write).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

	@Value("${chafgram.webauthn.rp-id:localhost}")
	private String rpId;

	@Value("${chafgram.webauthn.allowed-origins:http://localhost:3000}")
	private List<String> allowedOrigins;

	/**
	 * Actuator endpoints get their own chain (FR-10.4).
	 *
	 * <p>Health is a public liveness probe; {@code info} and {@code metrics}
	 * are admin-only. HTTP Basic is the only entry point here, so an
	 * anonymous request gets a {@code 401} with a {@code WWW-Authenticate}
	 * challenge instead of the member-facing login redirect — the global
	 * chain's form-login entry point would answer {@code 302} and send
	 * monitoring agents to an HTML login page.</p>
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
	 * Double-submit CSRF for API/BFF clients.
	 *
	 * <p>{@link CookieCsrfTokenRepository} issues the {@code XSRF-TOKEN} cookie
	 * (JavaScript-readable) and Spring compares it with the {@code X-XSRF-TOKEN}
	 * header. The default request handler is
	 * {@code XorCsrfTokenRequestAttributeHandler}, which only accepts a
	 * server-side XOR-masked token — a value a BFF or browser client can never
	 * obtain from the cookie — so the plain {@link CsrfTokenRequestAttributeHandler}
	 * is used instead: the cookie value is accepted verbatim. Chafgram serves
	 * JSON APIs only (no HTML forms reflecting the token), so BREACH masking
	 * buys nothing here.</p>
	 */
	@Bean
	@Order(2)
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/api/v1/workspace").permitAll()
				.requestMatchers("/api/v1/bootstrap", "/api/v1/register", "/api/v1/invites/accept")
					.permitAll()
				.requestMatchers("/login", "/login/webauthn", "/webauthn/authenticate/options",
						"/error").permitAll()
				// GraphQL and GraphiQL are public at the HTTP boundary; authorization
				// is enforced per resolver (class-level @PreAuthorize("isFullyAuthenticated()"),
				// with "permitAll()" on explicit public operations only).
				.requestMatchers("/graphql", "/graphiql").permitAll()
				.anyRequest().authenticated())
				.formLogin(Customizer.withDefaults())
			.webAuthn(webAuthn -> webAuthn
				.rpId(rpId)
				.allowedOrigins(allowedOrigins.toArray(new String[0])));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	JdbcPublicKeyCredentialUserEntityRepository userEntityRepository(JdbcOperations jdbc) {
		return new JdbcPublicKeyCredentialUserEntityRepository(jdbc);
	}

	@Bean
	JdbcUserCredentialRepository userCredentialRepository(JdbcOperations jdbc) {
		return new JdbcUserCredentialRepository(jdbc);
	}
}
