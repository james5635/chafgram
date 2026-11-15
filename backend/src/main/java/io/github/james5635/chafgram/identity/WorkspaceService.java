package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.dto.UpdateWorkspaceRequest;
import io.github.james5635.chafgram.identity.dto.WorkspaceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * The single-workspace configuration of this deployment. Public read access;
 * updates require an admin (and an active account).
 */
@Service
public class WorkspaceService {

	private final WorkspaceSettingsRepository settings;
	private final MemberAccess access;

	public WorkspaceService(WorkspaceSettingsRepository settings, MemberAccess access) {
		this.settings = settings;
		this.access = access;
	}

	@Transactional(readOnly = true)
	public WorkspaceResponse get() {
		return toResponse(singleton());
	}

	@Transactional
	public WorkspaceResponse update(String actorUsername, UpdateWorkspaceRequest request) {
		access.requireWriteable(actorUsername);
		WorkspaceSettings workspace = singleton();

		if (request.name() != null) {
			String name = Validation.requireDisplayName(request.name());
			workspace.setName(name);
		}
		if (request.slug() != null) {
			workspace.setSlug(Validation.requireSlug(request.slug()));
		}
		if (request.primaryColor() != null) {
			workspace.setPrimaryColor(Validation.optionalColor(request.primaryColor()));
		}
		if (request.signupMode() != null) {
			workspace.setSignupMode(parseSignupMode(request.signupMode()));
		}

		settings.save(workspace);
		return toResponse(workspace);
	}

	private WorkspaceSettings singleton() {
		return settings.findById(WorkspaceSettings.SINGLETON_ID)
				.orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
						"workspace_settings singleton (id=1) is missing"));
	}

	private SignupMode parseSignupMode(String raw) {
		try {
			return SignupMode.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
		}
		catch (IllegalArgumentException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST,
					"signupMode must be one of: invite_only, open");
		}
	}

	static WorkspaceResponse toResponse(WorkspaceSettings workspace) {
		return new WorkspaceResponse(
				workspace.getName(),
				workspace.getSlug(),
				workspace.getPrimaryColor(),
				workspace.getSignupMode().name().toLowerCase());
	}
}
