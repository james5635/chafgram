package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.identity.dto.UpdateWorkspaceRequest;
import io.github.james5635.chafgram.identity.dto.WorkspaceResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Workspace configuration. Publicly readable (branding is not secret);
 * updates require an admin.
 */
@RestController
public class WorkspaceController {

	private final WorkspaceService workspaceService;

	public WorkspaceController(WorkspaceService workspaceService) {
		this.workspaceService = workspaceService;
	}

	@GetMapping("/api/v1/workspace")
	public WorkspaceResponse get() {
		return workspaceService.get();
	}

	@PatchMapping("/api/v1/workspace")
	@PreAuthorize("hasRole('ADMIN')")
	public WorkspaceResponse update(@RequestBody UpdateWorkspaceRequest request) {
		return workspaceService.update(currentUsername(), request);
	}

	private String currentUsername() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}
}
