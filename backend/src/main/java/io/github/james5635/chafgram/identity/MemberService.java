package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.dto.MemberResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

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

	public MemberService(UserRepository users, MemberAccess access, PasswordEncoder passwordEncoder,
			WorkspaceSettingsRepository settings) {
		this.users = users;
		this.access = access;
		this.passwordEncoder = passwordEncoder;
		this.settings = settings;
	}

	@Transactional(readOnly = true)
	public MemberResponse currentMemberResponse(String username) {
		return toResponse(access.currentMember(username));
	}

	@Transactional(readOnly = true)
	public List<MemberResponse> listMembers() {
		return users.findAll().stream()
				.sorted(Comparator.comparing(User::getUsername))
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

	User findOrThrow(Long id) {
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

	static MemberResponse toResponse(User user) {
		return new MemberResponse(
				user.getId(),
				user.getUsername(),
				user.getDisplayName(),
				user.getEmail(),
				user.getRole().name().toLowerCase(),
				user.getStatus().name().toLowerCase(),
				user.getCreatedAt());
	}
}
