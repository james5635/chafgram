package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserRepository;
import io.github.james5635.chafgram.identity.WorkspaceSettingsRepository;
import io.github.james5635.chafgram.messaging.ConversationMemberRepository;
import io.github.james5635.chafgram.messaging.Message;
import io.github.james5635.chafgram.messaging.MessageMedia;
import io.github.james5635.chafgram.messaging.MessageMediaRepository;
import io.github.james5635.chafgram.messaging.MessageRepository;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.PostMedia;
import io.github.james5635.chafgram.social.PostMediaRepository;
import io.github.james5635.chafgram.social.PostRepository;
import io.github.james5635.chafgram.social.UserBlockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Authorization for media byte access via {@code GET /api/v1/media/{id}/bytes}.
 * <p>
 * Decision tree (first match wins):
 * <ol>
 *   <li>Asset is deleted (admin moderation, BR-5.3) → deny (404) for everyone.</li>
 *   <li>Viewer is admin/moderator → allow.</li>
 *   <li>Viewer owns the asset → allow.</li>
 *   <li>Asset attached to a message → viewer is sender or conversation member → allow.</li>
 *   <li>Asset attached to a post → post is non-tombstoned and viewer is not blocked by author → allow.</li>
 *   <li>Asset is avatar or workspace logo → allow (any member).</li>
 *   <li>Deny → identical 404 for all deny branches (no information leakage).</li>
 * </ol>
 */
@Service
public class MediaAccessService {

	private final MessageMediaRepository messageMedia;
	private final MessageRepository messages;
	private final ConversationMemberRepository conversationMembers;
	private final PostMediaRepository postMedia;
	private final PostRepository posts;
	private final UserBlockRepository blocks;
	private final UserRepository users;
	private final WorkspaceSettingsRepository workspaceSettings;

	public MediaAccessService(MessageMediaRepository messageMedia,
			MessageRepository messages,
			ConversationMemberRepository conversationMembers,
			PostMediaRepository postMedia,
			PostRepository posts,
			UserBlockRepository blocks,
			UserRepository users,
			WorkspaceSettingsRepository workspaceSettings) {
		this.messageMedia = messageMedia;
		this.messages = messages;
		this.conversationMembers = conversationMembers;
		this.postMedia = postMedia;
		this.posts = posts;
		this.blocks = blocks;
		this.users = users;
		this.workspaceSettings = workspaceSettings;
	}

	/**
	 * Verify that {@code viewer} may read {@code asset} bytes.
	 * Throws {@link ApiException(HttpStatus.NOT_FOUND, "Media not found")} on deny
	 * — identical for all deny branches (no information leakage).
	 */
	public void assertCanView(User viewer, MediaAsset asset) {
		// 0. Deleted by an admin (BR-5.3) → gone for everyone, staff included
		if (asset.getStatus() == MediaStatus.DELETED) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Media not found");
		}

		// 1. Staff → allow
		if (viewer.getRole() == Role.ADMIN
				|| viewer.getRole() == Role.MODERATOR) {
			return;
		}

		// 2. Owner → allow
		if (asset.getOwnerUserId().equals(viewer.getId())) {
			return;
		}

		// 3. Message media → viewer is sender or conversation member → allow
		List<MessageMedia> mmList = messageMedia.findByMediaId(asset.getId());
		for (MessageMedia mm : mmList) {
			Message msg = messages.findById(mm.getMessageId()).orElse(null);
			if (msg != null) {
				// null-safe: a purged sender leaves sender_id NULL
				boolean isSender = viewer.getId().equals(msg.getSenderId());
				// active membership only — a member who left keeps the row
				boolean isMember = conversationMembers
						.findActiveByConversationAndUser(msg.getConversationId(), viewer.getId())
						.isPresent();
				if (isSender || isMember) {
					return;
				}
			}
		}

		// 4. Post media → post visible and not blocked → allow
		List<PostMedia> pmList = postMedia.findByMediaId(asset.getId());
		if (!pmList.isEmpty()) {
			for (PostMedia pm : pmList) {
				Post post =
						posts.findVisibleById(pm.getPostId()).orElse(null);
				if (post != null) {
					boolean blocked = blocks.isBlockedBy(post.getAuthorId(), viewer.getId())
							|| blocks.isBlockedBy(viewer.getId(), post.getAuthorId());
					if (!blocked) {
						return;
					}
				}
			}
		}

		// 5. Avatar or workspace logo → allow (any member)
		if (isAvatarOrLogo(viewer, asset)) {
			return;
		}

		// 6. Deny → identical 404
		throw new ApiException(HttpStatus.NOT_FOUND, "Media not found");
	}

	private boolean isAvatarOrLogo(User viewer, MediaAsset asset) {
		// Asset is avatar of any member — check all users
		List<User> allUsers = users.findAll();
		for (User u : allUsers) {
			if (u.getAvatarMediaId() != null && u.getAvatarMediaId().equals(asset.getId())) {
				return true;
			}
		}
		// Asset is workspace logo
		return workspaceSettings.findById((byte) 1)
				.map(ws -> ws.getLogoMediaId() != null && ws.getLogoMediaId().equals(asset.getId()))
				.orElse(false);
	}
}
