package io.github.james5635.chafgram.identity.auth;

/**
 * The credentials returned by login and refresh.
 *
 * @param accessToken short-lived bearer JWT
 * @param refreshToken opaque Base64URL token, HTTP-only cookie at the BFF boundary
 * @param expiresIn access-token lifetime in seconds
 */
public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
}
