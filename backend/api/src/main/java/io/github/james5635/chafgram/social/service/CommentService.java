package io.github.james5635.chafgram.social.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.common.service.AuditService;
import java.util.Map;
import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.notifications.NotificationEntityType;
import io.github.james5635.chafgram.notifications.service.NotificationService;
import io.github.james5635.chafgram.notifications.NotificationType;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.james5635.chafgram.social.repository.CommentRepository;
import io.github.james5635.chafgram.social.Comment;
import io.github.james5635.chafgram.social.Post;

/** Comments: one level of threading, tombstone deletes, @mention notifications (BR-4.x). */
@Service
public class CommentService {

	static final int MAX_BODY = 2000;
	private static final Pattern MENTION = Pattern.compile("@([a-z0-9][a-z0-9-]{2,29})");

	private final CommentRepository comments;
	private final PostService postService;
	private final UserRepository users;
	private final NotificationService notifications;
	private final AuditService audit;
	private final MemberAccess access;

	public CommentService(CommentRepository comments, PostService postService,
			UserRepository users, NotificationService notifications, AuditService audit,
			MemberAccess access) {
		this.comments = comments;
		this.postService = postService;
		this.users = users;
		this.notifications = notifications;
		this.audit = audit;
		this.access = access;
	}

	@Transactional
	public Comment add(User actor, Long postId, Long parentId, String body) {
		access.requireWriteable(actor);
		if (body == null || body.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Comment body is required");
		}
		if (body.length() > MAX_BODY) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Comment body is limited to " + MAX_BODY + " characters");
		}
		Post post = postService.findVisible(postId); // BR-4.4: tombstoned posts accept no comments

		if (parentId != null) {
			Comment parent = comments.findVisibleById(parentId)
					.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Parent comment not found"));
			if (parent.getParentId() != null) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Replies can only target top-level comments"); // BR-4.1
			}
			if (!parent.getPostId().equals(postId)) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Reply parent belongs to another post");
			}
		}

		Comment comment = new Comment();
		comment.setPostId(postId);
		comment.setAuthorId(actor.getId());
		comment.setParentId(parentId);
		comment.setBody(body.trim());
		comments.save(comment);

		// notifications: post author on a comment; parent comment author on a reply; @mentions on anyone
		if (parentId == null) {
			notifications.notify(post.getAuthorId(), actor.getId(), NotificationType.COMMENT, NotificationEntityType.POST, post.getId());
		}
		else {
			var parent = comments.findById(parentId).orElseThrow();
			notifications.notify(parent.getAuthorId(), actor.getId(), NotificationType.REPLY, NotificationEntityType.COMMENT, parent.getId());
		}
		for (String username : mentions(comment.getBody())) {
			users.findByUsername(username).ifPresent(mentioned -> notifications.notify(
					mentioned.getId(), actor.getId(), NotificationType.MENTION, NotificationEntityType.COMMENT, comment.getId()));
		}
		return comment;
	}

	@Transactional
	public Comment edit(User actor, Long commentId, String body) {
		access.requireWriteable(actor);
		Comment comment = findVisible(commentId);
		if (!comment.getAuthorId().equals(actor.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Only the author can edit this comment");
		}
		if (body == null || body.isBlank() || body.length() > MAX_BODY) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Comment body is required (max " + MAX_BODY + " chars)");
		}
		comment.setBody(body.trim());
		comment.setEditedAt(Instant.now());
		return comments.save(comment);
	}

	@Transactional
	public void delete(User actor, Long commentId) {
		access.requireWriteable(actor);
		Comment comment = comments.findById(commentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found"));
		boolean isAuthor = comment.getAuthorId().equals(actor.getId());
		boolean isStaff = actor.getRole() == Role.ADMIN || actor.getRole() == Role.MODERATOR;
		if (!isAuthor && !isStaff) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Only the author or a moderator can delete this comment");
		}
		if (comment.getDeletedAt() == null) {
			comment.setDeletedAt(Instant.now()); // tombstone keeps the thread readable (BR-4.2)
			comments.save(comment);
			if (isStaff && !isAuthor) {
				audit.log(actor.getId(), "comment.delete", "comment", comment.getId(),
						Map.of("authorId", comment.getAuthorId(), "postId", comment.getPostId(), "staffAction", true));
			}
		}
	}

	@Transactional(readOnly = true)
	public Comment getVisible(Long commentId) {
		return findVisible(commentId);
	}

	@Transactional(readOnly = true)
	public List<Comment> listByPost(Long postId, int limit) {
		return comments.findByPost(postId, PageRequest.of(0, Math.min(Math.max(limit, 1), 200)));
	}

	@Transactional(readOnly = true)
	public long countByPost(Long postId) {
		return comments.countVisibleByPost(postId);
	}

	private Comment findVisible(Long commentId) {
		return comments.findVisibleById(commentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found or removed"));
	}

	private List<String> mentions(String text) {
		Set<String> found = new LinkedHashSet<>();
		Matcher matcher = MENTION.matcher(text);
		while (matcher.find()) {
			found.add(matcher.group(1));
		}
		return List.copyOf(found);
	}
}
