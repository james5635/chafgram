package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.mail.ChafgramMailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Email verification and password reset (FR-1.7): single-use, time-boxed,
 * SHA-256-hashed tokens.
 */
class EmailFlowsIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private AccountFlowService accountFlows;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository users;

	@MockitoBean
	private ChafgramMailSender mail;

	User admin;
	User member;

	@BeforeEach
	void setUp() {
		wipeSocial();
		admin = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		CreateInviteResponse invite = inviteService.create(null, Role.MEMBER, admin);
		member = inviteService.accept(invite.token(),
				"bob", "Bob", "bob@example.com", "bob-very-strong-password");
	}

	// ── password reset ───────────────────────────────────────────────────────

	@Test
	void passwordResetForUnknownAddressSucceedsSilentlyAndCreatesNoToken() {
		accountFlows.requestPasswordReset("nobody@example.com");

		assertThat(countTokens()).isZero();
		verify(mail, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void passwordResetForKnownAddressCreatesATokenRow() {
		accountFlows.requestPasswordReset("bob@example.com");

		ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(mail).send(to.capture(), subject.capture(), body.capture());

		assertThat(to.getValue()).isEqualTo("bob@example.com");
		assertThat(subject.getValue()).containsIgnoringCase("password");
		assertThat(body.getValue()).contains("token=");

		assertThat(countTokens()).isEqualTo(1);
		assertThat(countTokensOfType("reset_password")).isEqualTo(1);
	}

	@Test
	void storedTokenIsTheSha256HashOfTheRawToken() {
		accountFlows.requestPasswordReset("bob@example.com");
		String raw = extractTokenFromMail();

		String stored = jdbc.queryForObject("select token_hash from account_tokens limit 1", String.class);

		assertThat(stored).isEqualTo(sha256Hex(raw));
		assertThat(stored).isNotEqualTo(raw);
		assertThat(stored).hasSize(64);
	}

	@Test
	void resetPasswordRejectsShortPasswords() {
		accountFlows.requestPasswordReset("bob@example.com");
		String raw = extractTokenFromMail();

		assertThatThrownBy(() -> accountFlows.resetPassword(raw, "short"))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void validResetReplacesThePasswordHash() {
		accountFlows.requestPasswordReset("bob@example.com");
		String raw = extractTokenFromMail();

		String before = users.findByUsername("bob").orElseThrow().getPasswordHash();

		accountFlows.resetPassword(raw, "a-brand-new-strong-password");

		String after = users.findByUsername("bob").orElseThrow().getPasswordHash();
		assertThat(after).isNotEqualTo(before);
		assertThat(passwordEncoder.matches("a-brand-new-strong-password", after)).isTrue();
	}

	@Test
	void resetTokenIsSingleUse() {
		accountFlows.requestPasswordReset("bob@example.com");
		String raw = extractTokenFromMail();

		accountFlows.resetPassword(raw, "a-brand-new-strong-password");

		assertThatThrownBy(() -> accountFlows.resetPassword(raw, "another-strong-password"))
				.isInstanceOf(ApiException.class);

		assertThat(usedAtCount()).isEqualTo(1);
	}

	@Test
	void expiredResetTokenIsRejected() {
		accountFlows.requestPasswordReset("bob@example.com");
		String raw = extractTokenFromMail();

		jdbc.update("update account_tokens set expires_at = ?",
				Timestamp.from(Instant.now().minusSeconds(60)));

		assertThatThrownBy(() -> accountFlows.resetPassword(raw, "a-brand-new-strong-password"))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void unknownResetTokenIsRejected() {
		assertThatThrownBy(() -> accountFlows.resetPassword("no-such-token", "a-brand-new-strong-password"))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void concurrentRedemptionsOfTheSameTokenOnlySucceedOnce() throws Exception {
		accountFlows.requestPasswordReset("bob@example.com");
		String raw = extractTokenFromMail();

		int threads = 4;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			CountDownLatch start = new CountDownLatch(1);
			List<Future<Boolean>> results = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				results.add(pool.submit(() -> {
					start.await();
					try {
						accountFlows.resetPassword(raw, "a-brand-new-strong-password");
						return true;
					}
					catch (ApiException e) {
						return false;
					}
				}));
			}
			start.countDown();

			int winners = 0;
			for (Future<Boolean> result : results) {
				if (result.get(30, TimeUnit.SECONDS)) {
					winners++;
				}
			}

			assertThat(winners).as("a single-use token must be redeemable once").isEqualTo(1);
			assertThat(usedAtCount()).isEqualTo(1);
		}
		finally {
			pool.shutdownNow();
		}
	}

	// ── email verification ───────────────────────────────────────────────────

	@Test
	void verifyEmailSetsTheVerifiedTimestamp() {
		accountFlows.requestEmailVerification(member);
		String raw = extractTokenFromMail();

		accountFlows.verifyEmail(raw);

		User bob = users.findByUsername("bob").orElseThrow();
		assertThat(bob.getEmailVerifiedAt()).isNotNull();
	}

	@Test
	void verificationTokenIsSingleUse() {
		accountFlows.requestEmailVerification(member);
		String raw = extractTokenFromMail();

		accountFlows.verifyEmail(raw);

		assertThatThrownBy(() -> accountFlows.verifyEmail(raw))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void verificationTokenCannotBeUsedForAPasswordReset() {
		accountFlows.requestEmailVerification(member);
		String raw = extractTokenFromMail();

		assertThatThrownBy(() -> accountFlows.resetPassword(raw, "a-brand-new-strong-password"))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void resetTokenCannotBeUsedForEmailVerification() {
		accountFlows.requestPasswordReset("bob@example.com");
		String raw = extractTokenFromMail();

		assertThatThrownBy(() -> accountFlows.verifyEmail(raw))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void verificationIsNotRequestedTwiceOnceConfirmed() {
		accountFlows.requestEmailVerification(member);
		accountFlows.verifyEmail(extractTokenFromMail());
		int sentAfterFirst = sentCount();

		accountFlows.requestEmailVerification(users.findByUsername("bob").orElseThrow());

		assertThat(sentCount()).isEqualTo(sentAfterFirst);
	}

	// ── GraphQL surface ─────────────────────────────────────────────────────

	@Test
	void requestPasswordResetMutationIsPublic() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { requestPasswordReset(email: \\\"bob@example.com\\\") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.requestPasswordReset", is(true)));

		assertThat(countTokens()).isEqualTo(1);
	}

	@Test
	void requestPasswordResetMutationDoesNotRevealUnknownAddresses() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { requestPasswordReset(email: \\\"nobody@example.com\\\") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.requestPasswordReset", is(true)));

		assertThat(countTokens()).isZero();
	}

	@Test
	void resetPasswordMutationIsPublic() throws Exception {
		accountFlows.requestPasswordReset("bob@example.com");
		String raw = extractTokenFromMail();

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { resetPassword(token: \\\"" + raw
								+ "\\\", password: \\\"a-brand-new-strong-password\\\") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.resetPassword", is(true)));

		assertThat(passwordEncoder.matches("a-brand-new-strong-password",
				users.findByUsername("bob").orElseThrow().getPasswordHash())).isTrue();
	}

	@Test
	void verifyEmailMutationIsPublic() throws Exception {
		accountFlows.requestEmailVerification(member);
		String raw = extractTokenFromMail();

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { verifyEmail(token: \\\"" + raw + "\\\") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.verifyEmail", is(true)));

		assertThat(users.findByUsername("bob").orElseThrow().getEmailVerifiedAt()).isNotNull();
	}

	@Test
	void requestEmailVerificationMutationNeedsAuthentication() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { requestEmailVerification }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("UNAUTHORIZED")));
	}

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void requestEmailVerificationMutationMailsTheMember() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { requestEmailVerification }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.requestEmailVerification", is(true)));

		ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
		verify(mail).send(to.capture(), anyString(), anyString());
		assertThat(to.getValue()).isEqualTo("bob@example.com");
	}

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void meExposesTheVerifiedTimestamp() throws Exception {
		accountFlows.requestEmailVerification(member);
		accountFlows.verifyEmail(extractTokenFromMail());

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ me { username emailVerifiedAt } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.me.username", is("bob")))
				.andExpect(jsonPath("$.data.me.emailVerifiedAt", notNullValue()));
	}

	// ── helpers ─────────────────────────────────────────────────────────────

	private int countTokens() {
		return jdbc.queryForObject("select count(*) from account_tokens", Integer.class);
	}

	private int countTokensOfType(String type) {
		return jdbc.queryForObject("select count(*) from account_tokens where type = ?",
				Integer.class, type);
	}

	private int usedAtCount() {
		return jdbc.queryForObject("select count(*) from account_tokens where used_at is not null",
				Integer.class);
	}

	private int sentCount() {
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(mail, atLeast(0)).send(anyString(), anyString(), body.capture());
		return body.getAllValues().size();
	}

	/** Pulls the raw token out of the captured mail body. */
	private String extractTokenFromMail() {
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(mail, atLeastOnce()).send(anyString(), anyString(), body.capture());
		String text = body.getValue();
		int marker = text.indexOf("token=");
		assertThat(marker).isGreaterThan(-1);
		String tail = text.substring(marker + "token=".length());
		int end = tail.indexOf('\n');
		return (end < 0 ? tail : tail.substring(0, end)).trim();
	}

	private static String sha256Hex(String value) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
