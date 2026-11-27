package io.github.james5635.chafgram.identity.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.identity.dto.InviteResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import io.github.james5635.chafgram.identity.repository.InviteRepository;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.utils.Tokens;
import io.github.james5635.chafgram.identity.utils.Validation;
import io.github.james5635.chafgram.identity.Invite;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserStatus;

/**
 * Workspace invites. Raw bearer tokens are never stored — only their SHA-256
 * hex digest. Invites are single-use, time-boxed, and default to member role.
 */
@Service
public class InviteService {

	private static final Duration DEFAULT_TTL = Duration.ofDays(7);

	private final InviteRepository invites;
	private final UserRepository users;
	private final MemberAccess access;
	private final PasswordEncoder passwordEncoder;

	public InviteService(InviteRepository invites, UserRepository users, MemberAccess access,
			PasswordEncoder passwordEncoder) {
		this.invites = invites;
		this.users = users;
		this.access = access;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public CreateInviteResponse create(String email, Role role, User creator) {
		// Re-read the role from the database: a demoted admin's JWT can still
		// carry ROLE_ADMIN until it expires (FR-1.3).
		User actor = access.requireAdmin(creator.getUsername());

		String token = Tokens.newBearerToken();
		Invite invite = new Invite();
		invite.setEmail(Validation.normalizeEmail(email));
		invite.setTokenHash(Tokens.sha256Hex(token));
		invite.setRole(role != null ? role : Role.MEMBER);
		invite.setCreatedBy(actor.getId());
		invite.setExpiresAt(Instant.now().plus(DEFAULT_TTL));
		invites.save(invite);

		return new CreateInviteResponse(
				invite.getId(),
				invite.getEmail(),
				invite.getRole().name().toLowerCase(),
				token,
				"/join?token=" + token,
				invite.getExpiresAt());
	}

	@Transactional(readOnly = true)
	public List<InviteResponse> list() {
		return invites.findAllByOrderByCreatedAtDesc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public void revoke(Long id, User actor) {
		access.requireAdmin(actor.getUsername());
		if (!invites.existsById(id)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Invite not found");
		}
		invites.deleteById(id);
	}

	/**
	 * Claims an invite. Email comes from the invite when it is email-bound,
	 * otherwise from the request (required for reusable links).
	 */
	@Transactional
	public User accept(String token, String username, String displayName, String email,
			String password) {
		if (token == null || token.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Invite token is required");
		}
		Invite invite = invites.findByTokenHash(Tokens.sha256Hex(token))
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid invite token"));
		if (invite.getAcceptedAt() != null) {
			throw new ApiException(HttpStatus.CONFLICT, "Invite has already been used");
		}
		if (Instant.now().isAfter(invite.getExpiresAt())) {
			throw new ApiException(HttpStatus.GONE, "Invite has expired");
		}

		String normalizedEmail = invite.getEmail() != null
				? invite.getEmail()
				: Validation.normalizeEmail(email);
		if (normalizedEmail == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required for this invite");
		}

		String normalizedUsername = Validation.requireUsername(username);
		String normalizedDisplayName = Validation.requireDisplayName(displayName);
		String normalizedPassword = Validation.normalizePassword(password);

		if (users.existsByUsername(normalizedUsername)) {
			throw new ApiException(HttpStatus.CONFLICT, "Username is already taken");
		}
		if (users.existsByEmail(normalizedEmail)) {
			throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
		}

		User user = new User();
		user.setUsername(normalizedUsername);
		user.setDisplayName(normalizedDisplayName);
		user.setEmail(normalizedEmail);
		user.setRole(invite.getRole());
		user.setStatus(UserStatus.ACTIVE);
		if (normalizedPassword != null) {
			user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
		}
		users.save(user);

		invite.setAcceptedAt(Instant.now());
		invites.save(invite);

		return user;
	}

	private InviteResponse toResponse(Invite invite) {
		return new InviteResponse(
				invite.getId(),
				invite.getEmail(),
				invite.getRole().name().toLowerCase(),
				invite.getCreatedAt(),
				invite.getExpiresAt(),
				invite.getAcceptedAt());
	}
}
