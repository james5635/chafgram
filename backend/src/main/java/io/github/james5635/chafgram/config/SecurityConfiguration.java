package io.github.james5635.chafgram.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/api/v1/workspace").permitAll()
				.requestMatchers("/api/v1/bootstrap", "/api/v1/register", "/api/v1/invites/accept")
					.permitAll()
				.requestMatchers("/actuator/health", "/actuator/info").permitAll()
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
