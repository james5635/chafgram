package io.github.james5635.chafgram.identity;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * Public account mutations — no authentication required (FR-1.7).
 * <p>
 * These are the only operations a member can run before signing in: they
 * redeem a token that was mailed to them. Every other resolver class carries
 * class-level {@code @PreAuthorize("isFullyAuthenticated()")}.
 */
@Controller
@PreAuthorize("permitAll()")
public class PublicMutationResolver {

	private final AccountFlowService accountFlows;

	public PublicMutationResolver(AccountFlowService accountFlows) {
		this.accountFlows = accountFlows;
	}

	/**
	 * Starts a password reset. Always reports success so the endpoint cannot
	 * be used to discover which addresses have accounts.
	 */
	@MutationMapping
	public boolean requestPasswordReset(@Argument String email) {
		accountFlows.requestPasswordReset(email);
		return true;
	}

	/** Redeems a reset token and replaces the password. */
	@MutationMapping
	public boolean resetPassword(@Argument String token, @Argument String password) {
		accountFlows.resetPassword(token, password);
		return true;
	}

	/** Redeems a verification token and marks the address as verified. */
	@MutationMapping
	public boolean verifyEmail(@Argument String token) {
		accountFlows.verifyEmail(token);
		return true;
	}
}
