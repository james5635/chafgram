package io.github.james5635.chafgram.social.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.notifications.NotificationEntityType;
import io.github.james5635.chafgram.notifications.service.NotificationService;
import io.github.james5635.chafgram.notifications.NotificationType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import io.github.james5635.chafgram.social.repository.CommentReactionRepository;
import io.github.james5635.chafgram.social.repository.CommentRepository;
import io.github.james5635.chafgram.social.repository.PostReactionRepository;
import io.github.james5635.chafgram.social.repository.PostRepository;
import io.github.james5635.chafgram.social.Comment;
import io.github.james5635.chafgram.social.CommentReaction;
import io.github.james5635.chafgram.social.CommentReactionId;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.PostReaction;
import io.github.james5635.chafgram.social.ReactionId;
import io.github.james5635.chafgram.social.ReactionType;

/** Reactions on posts and comments. One row per (user, target); switching type updates it. */
@Service
public class ReactionService {

	private final PostReactionRepository postReactions;
	private final CommentReactionRepository commentReactions;
	private final CommentRepository comments;
	private final PostRepository posts;
	private final NotificationService notifications;

	public ReactionService(PostReactionRepository postReactions, CommentReactionRepository commentReactions,
			CommentRepository comments, PostRepository posts, NotificationService notifications) {
		this.postReactions = postReactions;
		this.commentReactions = commentReactions;
		this.comments = comments;
		this.posts = posts;
		this.notifications = notifications;
	}

	@Transactional
	public void reactToPost(User actor, Long postId, ReactionType type) {
		Post post = posts.findVisibleById(postId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found or removed"));
		ReactionId id = new ReactionId(postId, actor.getId());
		boolean isNew = !postReactions.existsById(id);
		PostReaction reaction = postReactions.findById(id).orElseGet(() -> {
			PostReaction r = new PostReaction();
			r.setPostId(postId);
			r.setUserId(actor.getId());
			return r;
		});
		reaction.setType(type);
		postReactions.save(reaction);
		if (isNew) {
			notifications.notify(post.getAuthorId(), actor.getId(), NotificationType.LIKE, NotificationEntityType.POST, post.getId());
		}
	}

	@Transactional
	public void removeReactionFromPost(User actor, Long postId) {
		postReactions.deleteById(new ReactionId(postId, actor.getId()));
	}

	@Transactional
	public void reactToComment(User actor, Long commentId, ReactionType type) {
		Comment comment = comments.findVisibleById(commentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found or removed"));
		CommentReactionId id = new CommentReactionId(commentId, actor.getId());
		boolean isNew = !commentReactions.existsById(id);
		CommentReaction reaction = commentReactions.findById(id).orElseGet(() -> {
			CommentReaction r = new CommentReaction();
			r.setCommentId(commentId);
			r.setUserId(actor.getId());
			return r;
		});
		reaction.setType(type);
		commentReactions.save(reaction);
		if (isNew) {
			notifications.notify(comment.getAuthorId(), actor.getId(), NotificationType.LIKE, NotificationEntityType.COMMENT, comment.getId());
		}
	}

	@Transactional
	public void removeReactionFromComment(User actor, Long commentId) {
		commentReactions.deleteById(new CommentReactionId(commentId, actor.getId()));
	}

	@Transactional(readOnly = true)
	public long postReactionCount(Long postId) {
		return postReactions.countByPost(postId);
	}

	@Transactional(readOnly = true)
	public long commentReactionCount(Long commentId) {
		return commentReactions.countByComment(commentId);
	}

	@Transactional(readOnly = true)
	public Optional<ReactionType> myPostReaction(User actor, Long postId) {
		return actor == null ? Optional.empty() : postReactions.findTypeByPostAndUser(postId, actor.getId());
	}

	@Transactional(readOnly = true)
	public Optional<ReactionType> myCommentReaction(User actor, Long commentId) {
		return actor == null ? Optional.empty() : commentReactions.findTypeByCommentAndUser(commentId, actor.getId());
	}
}
