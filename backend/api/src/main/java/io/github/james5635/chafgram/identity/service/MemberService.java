package io.github.james5635.chafgram.identity.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.dto.MemberResponse;
import io.github.james5635.chafgram.media.MediaKind;
import io.github.james5635.chafgram.media.service.MediaValidation;
import io.github.james5635.chafgram.social.dto.MemberDto;
import io.github.james5635.chafgram.social.dto.Views;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.repository.WorkspaceSettingsRepository;
import io.github.james5635.chafgram.identity.utils.Validation;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.SignupMode;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserStatus;
import io.github.james5635.chafgram.identity.WorkspaceSettings;

/**
 * Member lifecycle: bootstrap, registration, listing, role and status
 * administration. Authorization for admin actions is enforced here (in
 * addition to controller-level `@PreAuthorize`) — defense in depth.
 */
@Service
public class MemberService {

	private final UserRepository users;
	private final MemberAccess access;
	private final PasswordEncoder passwordEncoder;
	private final WorkspaceSettingsRepository settings;
	private final MediaValidation mediaValidation;
	private final Views views;

	public MemberService(UserRepository users, MemberAccess access, PasswordEncoder passwordEncoder,
			WorkspaceSettingsRepository settings, MediaValidation mediaValidation, Views views) {
		this.users = users;
		this.access = access;
		this.passwordEncoder = passwordEncoder;
		this.settings = settings;
		this.mediaValidation = mediaValidation;
		this.views = views;
	}

	@Transactional(readOnly = true)
	public MemberResponse currentMemberResponse(String username) {
		return toResponse(access.currentMember(username));
	}

	@Transactional(readOnly = true)
	public List<MemberResponse> listMembers(String query, int first) {
		List<User> all = users.findAll().stream()
				.sorted(Comparator.comparing(User::getUsername))
				.toList();
		String q = (query == null || query.isBlank()) ? null : query.trim().toLowerCase();
		if (q != null) {
			all = all.stream()
					.filter(u -> u.getUsername().toLowerCase().startsWith(q)
							|| (u.getDisplayName() != null && u.getDisplayName().toLowerCase().startsWith(q)))
					.toList();
		}
		return all.stream()
				.limit(Math.max(first, 1))
				.map(MemberService::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public MemberResponse getMember(Long id) {
		return toResponse(findOrThrow(id));
	}

	/** First-run bootstrap: valid only while the workspace has zero members. */
	@Transactional
	public User bootstrap(String username, String displayName, String email, String password) {
		if (users.count() > 0) {
			throw new ApiException(HttpStatus.CONFLICT, "Workspace already has members");
		}
		return createUser(username, displayName, email, password, Role.ADMIN);
	}

	/** Self-serve registration, only when the workspace signup mode is `open`. */
	@Transactional
	public User registerOpen(String username, String displayName, String email, String password) {
		WorkspaceSettings workspace = settings.findById(WorkspaceSettings.SINGLETON_ID)
				.orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
						"workspace_settings singleton (id=1) is missing"));
		if (workspace.getSignupMode() != SignupMode.OPEN) {
			throw new ApiException(HttpStatus.FORBIDDEN,
					"Self-service registration is disabled for this workspace");
		}
		return createUser(username, displayName, email, password, Role.MEMBER);
	}

	/** Self-service profile edit (FR-2.1/FR-2.4). Suspended members blocked. */
	@Transactional
	public MemberDto updateProfile(String username, String displayName, String bio, Long avatarMediaId) {
		User actor = access.requireWriteable(username);
		if (displayName != null) {
			actor.setDisplayName(Validation.requireDisplayName(displayName));
		}
		if (bio != null) {
			if (bio.length() > 160) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Bio is limited to 160 characters");
			}
			actor.setBio(bio);
		}
		if (avatarMediaId != null) {
			actor.setAvatarMediaId(mediaValidation
					.validateAttachable(actor, List.of(avatarMediaId), 1, MediaKind.IMAGE)
					.get(0).getId());
		}
		users.save(actor);
		return views.member(actor, actor);
	}

	@Transactional
	public MemberResponse updateRole(String actorUsername, Long targetId, Role newRole) {
		User actor = requireActiveAdmin(actorUsername);
		User target = findOrThrow(targetId);

		guardLastActiveAdmin(target, null, newRole);
		target.setRole(newRole);
		users.save(target);
		return toResponse(target);
	}

	@Transactional
	public MemberResponse setStatus(String actorUsername, Long targetId, UserStatus newStatus) {
		User actor = requireActiveAdmin(actorUsername);
		User target = findOrThrow(targetId);

		if (actor.getId().equals(target.getId()) && newStatus != UserStatus.ACTIVE) {
			throw new ApiException(HttpStatus.CONFLICT,
					"You cannot suspend or deactivate your own account");
		}
		guardLastActiveAdmin(target, newStatus, null);

		target.setStatus(newStatus);
		users.save(target);
		return toResponse(target);
	}

	User createUser(String username, String displayName, String email, String password, Role role) {
		String normalizedUsername = Validation.requireUsername(username);
		String normalizedDisplayName = Validation.requireDisplayName(displayName);
		String normalizedEmail = Validation.normalizeEmail(email);
		String normalizedPassword = Validation.normalizePassword(password);
		if (normalizedEmail == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required");
		}
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
		user.setRole(role);
		user.setStatus(UserStatus.ACTIVE);
		if (normalizedPassword != null) {
			user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
		}
		return users.save(user);
	}

	public User findOrThrow(Long id) {
		return users.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found"));
	}

	private User requireActiveAdmin(String actorUsername) {
		User actor = access.requireWriteable(actorUsername);
		if (actor.getRole() != Role.ADMIN) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Requires admin");
		}
		return actor;
	}

	private void guardLastActiveAdmin(User target, UserStatus newStatus, Role newRole) {
		boolean targetIsActiveAdmin =
				target.getRole() == Role.ADMIN && target.getStatus() == UserStatus.ACTIVE;
		boolean demotesAdmin = newRole != null && newRole != Role.ADMIN;
		boolean disablesAdmin =
				newStatus != null && !newStatus.equals(UserStatus.ACTIVE);
		if (targetIsActiveAdmin && (demotesAdmin || disablesAdmin)
				&& users.countActiveAdmins() <= 1) {
			throw new ApiException(HttpStatus.CONFLICT,
					"Workspace must keep at least one active admin");
		}
	}

	public static MemberResponse toResponse(User user) {
		return new MemberResponse(
				user.getId(),
				user.getUsername(),
				user.getDisplayName(),
				user.getEmail(),
				user.getRole().name().toLowerCase(),
				user.getStatus().name().toLowerCase(),
				user.getEmailVerifiedAt(),
				user.getCreatedAt());
	}
}
