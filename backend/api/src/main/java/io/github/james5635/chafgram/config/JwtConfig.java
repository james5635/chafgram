package io.github.james5635.chafgram.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * HS256 signing and verification share one symmetric key so the service that
 * issues access JWTs and the resource-server filter that verifies them agree.
 */
@Configuration
public class JwtConfig {

	/** Throwaway dev default; deployments must set CHAFGRAM_JWT_SECRET. */
	@Value("${chafgram.jwt.secret:${CHAFGRAM_JWT_SECRET:chafgram-dev-only-jwt-secret-do-not-use-in-production}}")
	private String secret;

	@Value("${chafgram.jwt.issuer:chafgram}")
	private String issuer;

	@Value("${chafgram.jwt.audience:chafgram-web}")
	private String audience;

	public String issuer() {
		return issuer;
	}

	public String audience() {
		return audience;
	}

	/** Shared HMAC key for viewer-bound media URLs; never leaves the server. */
	public byte[] signingSecret() {
		return secret.getBytes(StandardCharsets.UTF_8);
	}

	@Bean
	JwtEncoder jwtEncoder() {
		return NimbusJwtEncoder.withSecretKey(secretKey()).build();
	}

	@Bean
	JwtDecoder jwtDecoder() {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey()).build();
		OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuer), new AudienceValidator(audience));
		decoder.setJwtValidator(validator);
		return decoder;
	}

	/**
	 * Logout-only decoder. Signature, issuer, and audience are still verified,
	 * but {@code JwtTimestampValidator} is deliberately omitted: a client idle
	 * past the short access TTL must still be able to revoke its much longer
	 * refresh token, otherwise the 14-day credential stays live with no way to
	 * kill it from the app. Never use this decoder for resource access.
	 */
	public JwtDecoder decoderWithoutTimestampValidation() {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey()).build();
		OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
				new JwtIssuerValidator(issuer), new AudienceValidator(audience));
		decoder.setJwtValidator(validator);
		return decoder;
	}

	private SecretKey secretKey() {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException("CHAFGRAM_JWT_SECRET must be at least 32 bytes");
		}
		return new SecretKeySpec(keyBytes, "HmacSHA256");
	}

	/** encoded aud claim is a list; accept a token only when it names this web client. */
	private static final class AudienceValidator implements OAuth2TokenValidator<Jwt> {

		private final String expected;

		private AudienceValidator(String expected) {
			this.expected = expected;
		}

		@Override
		public OAuth2TokenValidatorResult validate(Jwt jwt) {
			if (jwt.getAudience().contains(expected)) {
				return OAuth2TokenValidatorResult.success();
			}
			return OAuth2TokenValidatorResult.failure(new OAuth2Error(
					"invalid_token", "JWT audience must contain " + expected, null));
		}
	}
}
