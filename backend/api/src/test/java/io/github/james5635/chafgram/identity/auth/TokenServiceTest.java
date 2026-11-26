package io.github.james5635.chafgram.identity.auth;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserStatus;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.utils.Tokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Token issuance, hashing, rotation, revocation, and reuse detection against real MySQL. */
class TokenServiceTest extends AbstractIntegrationTest {

	@Autowired
	private TokenService tokenService;

	@Autowired
	private RefreshTokenRepository refreshTokens;

	@Autowired
	private UserRepository users;

	@Autowired
	private MemberService memberService;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void clean() {
		jdbc.update("DELETE FROM refresh_tokens");
		wipeSocial();
	}

	@Test
	void issueSignsAccessJwtAndStoresOnlyARefreshHash() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		Instant before = Instant.now();

		TokenPair pair = tokenService.issue(alice);

		Jwt access = jwtDecoder.decode(pair.accessToken());
		assertThat(access.getSubject()).isEqualTo(String.valueOf(alice.getId()));
		// iss is a plain identifier, not a URL, so read it as a raw claim.
		assertThat(access.getClaimAsString("iss")).isEqualTo("chafgram");
		assertThat(access.getAudience()).containsExactly("chafgram-web");
		assertThat(access.getClaimAsStringList("roles")).containsExactly("admin");
		assertThat(access.getIssuedAt()).isNotNull().isAfterOrEqualTo(before.minusSeconds(5));
		assertThat(access.getExpiresAt())
				.isAfter(before.plusSeconds(14 * 60))
				.isBefore(before.plusSeconds(16 * 60));
		assertThat(pair.expiresIn()).isEqualTo(15 * 60L);

		String hash = Tokens.sha256Hex(pair.refreshToken());
		assertThat(hash).hasSize(64).isNotEqualTo(pair.refreshToken());
		assertThat(pair.refreshToken()).matches("[A-Za-z0-9_-]{43}");
		RefreshToken stored = refreshTokens.findByTokenHash(hash).orElseThrow();
		assertThat(stored.getUserId()).isEqualTo(alice.getId());
		assertThat(stored.getExpiresAt())
				.isAfter(before.plusSeconds(13L * 24 * 60 * 60))
				.isBefore(before.plusSeconds(14L * 24 * 60 * 60 + 60));
		assertThat(stored.getRevokedAt()).isNull();
		assertThat(stored.getReplacedBy()).isNull();
		assertThat(refreshTokens.findAllByUserIdAndRevokedAtIsNull(alice.getId()))
				.extracting(RefreshToken::getTokenHash).containsExactly(hash);
	}

	@Test
	void rotatedTokenReplayIsRejectedWithoutForkingTheSession() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");

		TokenPair first = tokenService.issue(alice);
		TokenPair second = tokenService.refresh(first.refreshToken());

		assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
		RefreshToken oldRow = refreshTokens.findByTokenHash(Tokens.sha256Hex(first.refreshToken())).orElseThrow();
		RefreshToken newRow = refreshTokens.findByTokenHash(Tokens.sha256Hex(second.refreshToken())).orElseThrow();
		assertThat(oldRow.getRevokedAt()).isNotNull();
		assertThat(oldRow.getReplacedBy()).isEqualTo(newRow.getId());
		assertThat(newRow.getRevokedAt()).isNull();
		Instant firstRevokedAt = oldRow.getRevokedAt();

		// A superseded token is revoked: presenting it again must fail without
		// minting a second live successor or touching the original revocation time.
		assertThatThrownBy(() -> tokenService.refresh(first.refreshToken()))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

		RefreshToken afterReplay = refreshTokens.findByTokenHash(Tokens.sha256Hex(first.refreshToken())).orElseThrow();
		assertThat(afterReplay.getRevokedAt()).isEqualTo(firstRevokedAt);
		assertThat(afterReplay.getReplacedBy()).isEqualTo(newRow.getId());
		assertThat(refreshTokens.findAllByUserIdAndRevokedAtIsNull(alice.getId()))
				.extracting(RefreshToken::getTokenHash)
				.containsExactly(Tokens.sha256Hex(second.refreshToken()));
		assertThat(refreshTokens.findAll()).hasSize(2);
	}

	@Test
	void replayAfterRevokeAllForUserIsRejectedEvenWhenAncestorWasRotated() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");

		TokenPair first = tokenService.issue(alice);
		TokenPair second = tokenService.refresh(first.refreshToken());
		tokenService.revokeAllForUser(alice.getId());

		// The ancestor has replaced_by, but logout-everywhere revoked the whole
		// family: neither the ancestor nor its successor can mint a new token.
		assertThatThrownBy(() -> tokenService.refresh(first.refreshToken()))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
		assertThatThrownBy(() -> tokenService.refresh(second.refreshToken()))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

		assertThat(refreshTokens.findAllByUserIdAndRevokedAtIsNull(alice.getId())).isEmpty();
		assertThat(refreshTokens.findAll()).hasSize(2);
	}

	@Test
	void concurrentRefreshWithSameTokenMintsExactlyOneLiveSuccessor() throws Exception {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		TokenPair first = tokenService.issue(alice);

		CountDownLatch gate = new CountDownLatch(1);
		Callable<Object> refreshTask = () -> {
			gate.await();
			try {
				return tokenService.refresh(first.refreshToken());
			}
			catch (ApiException e) {
				return e;
			}
		};
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<Object> one = pool.submit(refreshTask);
			Future<Object> two = pool.submit(refreshTask);
			gate.countDown();
			List<Object> results = List.of(one.get(60, TimeUnit.SECONDS), two.get(60, TimeUnit.SECONDS));

			assertThat(results).filteredOn(TokenPair.class::isInstance).hasSize(1);
			assertThat(results).filteredOn(ApiException.class::isInstance).hasSize(1);
			TokenPair winner = (TokenPair) results.stream().filter(TokenPair.class::isInstance).findFirst().orElseThrow();
			ApiException loser = (ApiException) results.stream().filter(ApiException.class::isInstance).findFirst().orElseThrow();
			assertThat(loser.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);

			List<RefreshToken> live = refreshTokens.findAllByUserIdAndRevokedAtIsNull(alice.getId());
			assertThat(live).hasSize(1);
			assertThat(live.get(0).getTokenHash()).isEqualTo(Tokens.sha256Hex(winner.refreshToken()));
			RefreshToken oldRow = refreshTokens.findByTokenHash(Tokens.sha256Hex(first.refreshToken())).orElseThrow();
			assertThat(oldRow.getReplacedBy()).isEqualTo(live.get(0).getId());
			assertThat(refreshTokens.findAll()).hasSize(2);
		}
		finally {
			pool.shutdownNow();
		}
	}

	@Test
	void revokedTokenReuseRevokesAllUserTokens() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		TokenPair revoked = tokenService.issue(alice);
		TokenPair other = tokenService.issue(alice);

		tokenService.revoke(revoked.refreshToken());
		RefreshToken row = refreshTokens.findByTokenHash(Tokens.sha256Hex(revoked.refreshToken())).orElseThrow();
		assertThat(row.getRevokedAt()).isNotNull();
		assertThat(row.getReplacedBy()).isNull();

		assertThatThrownBy(() -> tokenService.refresh(revoked.refreshToken()))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

		assertThat(refreshTokens.findAllByUserIdAndRevokedAtIsNull(alice.getId())).isEmpty();
		assertThat(refreshTokens.findByTokenHash(Tokens.sha256Hex(other.refreshToken())).orElseThrow()
				.getRevokedAt()).isNotNull();
	}

	@Test
	void wrongUserTokenCannotRefresh() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		User bob = inviteMember(alice, "bob", "Bob", "bob@example.com");

		TokenPair alicePair = tokenService.issue(alice);
		TokenPair refreshed = tokenService.refresh(alicePair.refreshToken());

		// A refresh token authenticates only its own row: no caller-chosen user,
		// no token minted into another workspace member's identity.
		Jwt access = jwtDecoder.decode(refreshed.accessToken());
		assertThat(access.getSubject()).isEqualTo(String.valueOf(alice.getId()));
		assertThat(access.getClaimAsStringList("roles")).containsExactly(Role.ADMIN.name().toLowerCase());
		assertThat(refreshTokens.findAllByUserIdAndRevokedAtIsNull(bob.getId())).isEmpty();
	}

	@Test
	void deactivatedUserCannotRefresh() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		TokenPair pair = tokenService.issue(alice);

		alice.setStatus(UserStatus.DEACTIVATED);
		users.save(alice);

		assertThatThrownBy(() -> tokenService.refresh(pair.refreshToken()))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
		assertThat(refreshTokens.findAllByUserIdAndRevokedAtIsNull(alice.getId()))
				.extracting(RefreshToken::getTokenHash)
				.containsExactly(Tokens.sha256Hex(pair.refreshToken()));
	}

	@Test
	void expiredAndUnknownRefreshTokensAreRejected() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		RefreshToken expired = new RefreshToken();
		expired.setUserId(alice.getId());
		expired.setTokenHash(Tokens.sha256Hex("expired-refresh-token"));
		expired.setExpiresAt(Instant.now().minusSeconds(60));
		refreshTokens.save(expired);

		assertThatThrownBy(() -> tokenService.refresh("expired-refresh-token"))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
		assertThatThrownBy(() -> tokenService.refresh("never-issued"))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
		assertThat(refreshTokens.findAllByUserIdAndRevokedAtIsNull(alice.getId()))
				.extracting(RefreshToken::getTokenHash)
				.containsExactly(Tokens.sha256Hex("expired-refresh-token"));
	}

	@Test
	void revokeAllForUserLeavesOtherMembersUntouched() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		User bob = inviteMember(alice, "bob", "Bob", "bob@example.com");
		TokenPair alicePair = tokenService.issue(alice);
		TokenPair bobPair = tokenService.issue(bob);

		tokenService.revokeAllForUser(alice.getId());

		assertThat(refreshTokens.findAllByUserIdAndRevokedAtIsNull(alice.getId())).isEmpty();
		assertThat(refreshTokens.findAllByUserIdAndRevokedAtIsNull(bob.getId()))
				.extracting(RefreshToken::getTokenHash)
				.containsExactly(Tokens.sha256Hex(bobPair.refreshToken()));
		assertThat(refreshTokens.findByTokenHash(Tokens.sha256Hex(alicePair.refreshToken())).orElseThrow()
				.getRevokedAt()).isNotNull();
	}

	@Test
	void revokeIsIdempotentForUnknownTokens() {
		tokenService.revoke("never-issued");
		List<RefreshToken> all = refreshTokens.findAll();
		assertThat(all).isEmpty();
	}

	/** Only the first member can bootstrap; everyone else joins by invite. */
	private User inviteMember(User admin, String username, String displayName, String email) {
		CreateInviteResponse invite = inviteService.create(null, null, admin);
		return inviteService.accept(invite.token(), username, displayName, email, "a-very-strong-password");
	}
}
