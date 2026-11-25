package io.github.james5635.chafgram.media.controller;

import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.social.utils.CurrentMember;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import io.github.james5635.chafgram.media.service.MediaService;

/**
 * Media mutations. Uploads are a REST concern (multipart); the only GraphQL
 * mutation is the admin moderation delete (BR-5.3).
 */
@Controller
@PreAuthorize("isFullyAuthenticated()")
public class MediaMutationResolver {

	private final MediaService media;
	private final MemberAccess access;
	private final CurrentMember current;

	public MediaMutationResolver(MediaService media, MemberAccess access, CurrentMember current) {
		this.media = media;
		this.access = access;
		this.current = current;
	}

	/**
	 * Admin-only moderation delete: tombstones the asset so it is served
	 * nowhere, and writes an audit-log entry (BR-5.3, BR-9.2).
	 */
	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public boolean deleteMedia(@Argument Long id) {
		var admin = access.requireAdmin(current.require().getUsername());
		media.deleteForModeration(admin, id);
		return true;
	}
}
