package io.github.james5635.chafgram.social.dto;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.notifications.Notification;
import io.github.james5635.chafgram.notifications.NotificationType;
import io.github.james5635.chafgram.social.Comment;
import io.github.james5635.chafgram.social.service.CommentService;
import io.github.james5635.chafgram.social.service.FollowService;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.service.PostService;
import io.github.james5635.chafgram.social.repository.PostMediaRepository;
import io.github.james5635.chafgram.social.PostType;
import io.github.james5635.chafgram.social.service.ReactionService;
import io.github.james5635.chafgram.social.ReactionType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/** Assembles member/post/comment/notification views with per-viewer context. */
@Component
public class Views {

	private final UserRepository users;
	private final FollowService follows;
	private final CommentService comments;
	private final ReactionService reactions;
	private final PostService posts;
	private final PostMediaRepository postMedia;

	public Views(UserRepository users, FollowService follows, CommentService comments,
			ReactionService reactions, PostService posts, PostMediaRepository postMedia) {
		this.users = users;
		this.follows = follows;
		this.comments = comments;
		this.reactions = reactions;
		this.posts = posts;
		this.postMedia = postMedia;
	}

	public User findUser(Long id) {
		return users.findById(id).orElse(null);
	}

	public User requireUser(Long id) {
		User user = findUser(id);
		if (user == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Member not found");
		}
		return user;
	}

	public MemberDto member(User u, User viewer) {
		return new MemberDto(
				u.getId(), u.getUsername(), u.getDisplayName(), u.getBio(), u.getAvatarMediaId(),
				u.getRole().name().toLowerCase(), u.getStatus().name().toLowerCase(),
				u.getEmailVerifiedAt(), u.getLastSeenAt(), u.getCreatedAt(),
				follows.followerCount(u.getId()), follows.followingCount(u.getId()),
				viewer != null && follows.isFollowing(viewer.getId(), u.getId()));
	}

	public PostDto post(Post p, User viewer) {
		User author = requireUser(p.getAuthorId());
		boolean edited = p.getUpdatedAt() != null && p.getCreatedAt() != null
				&& p.getUpdatedAt().isAfter(p.getCreatedAt());
		return new PostDto(
				p.getId(), member(author, viewer), p.getType(), p.getBody(),
				edited ? p.getUpdatedAt() : null, p.getCreatedAt(),
				comments.countByPost(p.getId()), reactions.postReactionCount(p.getId()),
				reactions.myPostReaction(viewer, p.getId()).orElse(null),
				viewer != null && posts.isSaved(viewer.getId(), p.getId()),
				postMedia.findByPostIdOrderByPositionAsc(p.getId()).stream()
						.map(pm -> pm.getMediaId()).toList());
	}

	public List<PostDto> posts(List<Post> page, User viewer) {
		return page.stream().map(p -> post(p, viewer)).toList();
	}

	public CommentDto comment(Comment c, User viewer) {
		User author = requireUser(c.getAuthorId());
		boolean edited = c.getEditedAt() != null;
		return new CommentDto(
				c.getId(), member(author, viewer), c.getPostId(), c.getParentId(),
				c.getDeletedAt() == null ? c.getBody() : "[removed]",
				edited ? c.getEditedAt() : null, c.getCreatedAt(),
				reactions.commentReactionCount(c.getId()),
				reactions.myCommentReaction(viewer, c.getId()).orElse(null));
	}

	public List<CommentDto> comments(List<Comment> page, User viewer) {
		return page.stream().map(c -> comment(c, viewer)).toList();
	}

	public NotificationDto notification(Notification n, User viewer) {
		MemberDto actor = n.getActorId() == null ? null : member(requireUser(n.getActorId()), viewer);
		return new NotificationDto(
				n.getId(), actor, n.getType(),
				n.getEntityType() == null ? null : n.getEntityType().name().toLowerCase(),
				n.getEntityId(), n.getReadAt(), n.getCreatedAt());
	}

	public List<NotificationDto> notifications(List<Notification> page, User viewer) {
		return page.stream().map(n -> notification(n, viewer)).toList();
	}

	public String encodeCursor(long id) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
	}

	public long decodeCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return Long.MAX_VALUE;
		}
		try {
			return Long.parseLong(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
		}
		catch (IllegalArgumentException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid cursor");
		}
	}

	public String nextCursor(List<Post> page, String lastRequested) {
		if (page.isEmpty()) {
			return null;
		}
		return encodeCursor(page.get(page.size() - 1).getId());
	}

	public ReactionSummaryDto summary(Long count, ReactionType myReaction) {
		return new ReactionSummaryDto(count, myReaction);
	}
}
