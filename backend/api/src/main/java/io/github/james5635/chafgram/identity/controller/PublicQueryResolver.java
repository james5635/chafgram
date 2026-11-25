package io.github.james5635.chafgram.identity.controller;

import io.github.james5635.chafgram.identity.dto.WorkspaceDto;
import io.github.james5635.chafgram.identity.dto.WorkspaceResponse;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import io.github.james5635.chafgram.identity.service.WorkspaceService;

/**
 * Public GraphQL operations — no authentication required. Only pre-login-safe,
 * non-sensitive data may be exposed here (workspace branding for the login page).
 * Every other resolver class carries class-level @PreAuthorize("isFullyAuthenticated()").
 */
@Controller
@PreAuthorize("permitAll()")
public class PublicQueryResolver {

	private final WorkspaceService workspaceService;

	public PublicQueryResolver(WorkspaceService workspaceService) {
		this.workspaceService = workspaceService;
	}

	@QueryMapping
	public WorkspaceDto workspace() {
		WorkspaceResponse w = workspaceService.get();
		return new WorkspaceDto(w.name(), w.slug(), w.primaryColor(), w.signupMode(), w.logoMediaId());
	}
}
