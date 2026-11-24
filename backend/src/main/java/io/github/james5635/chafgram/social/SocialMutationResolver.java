package io.github.james5635.chafgram.social;
import io.github.james5635.chafgram.social.dto.Views;
import io.github.james5635.chafgram.social.dto.MemberDto;
import io.github.james5635.chafgram.social.dto.PostDto;
import io.github.james5635.chafgram.social.dto.CommentDto;
import io.github.james5635.chafgram.social.dto.NotificationDto;
import io.github.james5635.chafgram.social.dto.ReactionSummaryDto;
import io.github.james5635.chafgram.social.dto.FeedPageDto;
import io.github.james5635.chafgram.social.CurrentMember;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.messaging.MessagingService;
import io.github.james5635.chafgram.moderation.ReportService;
import io.github.james5635.chafgram.moderation.ReportTargetType;
import io.github.james5635.chafgram.moderation.ReportReason;
import io.github.james5635.chafgram.notifications.NotificationService;
import io.github.james5635.chafgram.notifications.NotificationType;
import io.github.james5635.chafgram.social.Comment;
import io.github.james5635.chafgram.social.CommentService;
import io.github.james5635.chafgram.social.FollowService;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.PostType;
import io.github.james5635.chafgram.social.PostService;
import io.github.james5635.chafgram.social.BlockService;
import io.github.james5635.chafgram.social.ReactionService;
import io.github.james5635.chafgram.social.ReactionType;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@PreAuthorize("isFullyAuthenticated()")
public class SocialMutationResolver {

	private final CurrentMember current;
	private final Views views;
	private final PostService posts;
	private final CommentService comments;
	private final FollowService follows;
	private final BlockService blocks;
	private final ReactionService reactions;
	private final NotificationService notifications;

	public SocialMutationResolver(CurrentMember current, Views views, PostService posts, CommentService comments,
			FollowService follows, BlockService blocks, ReactionService reactions,
			NotificationService notifications) {
		this.current = current;
		this.views = views;
		this.posts = posts;
		this.comments = comments;
		this.follows = follows;
		this.blocks = blocks;
		this.reactions = reactions;
		this.notifications = notifications;
	}

	@MutationMapping
	public PostDto createPost(@Argument PostType type, @Argument String body, @Argument List<Long> mediaIds) {
		User actor = current.require();
		Post post = posts.create(actor, type, body, mediaIds);
		return views.post(post, actor);
	}

	@MutationMapping
	public PostDto updatePost(@Argument Long id, @Argument String body) {
		User actor = current.require();
		Post post = posts.update(actor, id, body);
		return views.post(post, actor);
	}

	@MutationMapping
	public boolean deletePost(@Argument Long id) {
		posts.delete(current.require(), id);
		return true;
	}

	@MutationMapping
	public CommentDto createComment(@Argument Long postId, @Argument Long parentId, @Argument String body) {
		User actor = current.require();
		Comment comment = comments.add(actor, postId, parentId, body);
		return views.comment(comment, actor);
	}

	@MutationMapping
	public CommentDto editComment(@Argument Long id, @Argument String body) {
		User actor = current.require();
		Comment comment = comments.edit(actor, id, body);
		return views.comment(comment, actor);
	}

	@MutationMapping
	public boolean deleteComment(@Argument Long id) {
		comments.delete(current.require(), id);
		return true;
	}

	@MutationMapping
	public boolean follow(@Argument Long id) {
		follows.follow(current.require(), id);
		return true;
	}

	@MutationMapping
	public boolean unfollow(@Argument Long id) {
		follows.unfollow(current.require(), id);
		return true;
	}

	@MutationMapping
	public boolean blockMember(@Argument Long id) {
		blocks.block(current.require(), id);
		return true;
	}

	@MutationMapping
	public boolean unblockMember(@Argument Long id) {
		blocks.unblock(current.require(), id);
		return true;
	}

	@MutationMapping
	public ReactionSummaryDto reactToPost(@Argument Long id, @Argument ReactionType type) {
		User actor = current.require();
		reactions.reactToPost(actor, id, type);
		return new ReactionSummaryDto(reactions.postReactionCount(id), reactions.myPostReaction(actor, id).orElse(null));
	}

	@MutationMapping
	public boolean unreactToPost(@Argument Long id) {
		reactions.removeReactionFromPost(current.require(), id);
		return true;
	}

	@MutationMapping
	public ReactionSummaryDto reactToComment(@Argument Long id, @Argument ReactionType type) {
		User actor = current.require();
		reactions.reactToComment(actor, id, type);
		return new ReactionSummaryDto(reactions.commentReactionCount(id), reactions.myCommentReaction(actor, id).orElse(null));
	}

	@MutationMapping
	public boolean unreactToComment(@Argument Long id) {
		reactions.removeReactionFromComment(current.require(), id);
		return true;
	}

	@MutationMapping
	public boolean savePost(@Argument Long id) {
		posts.save(current.require(), id);
		return true;
	}

	@MutationMapping
	public boolean unsavePost(@Argument Long id) {
		posts.unsave(current.require(), id);
		return true;
	}

	@MutationMapping
	public boolean markNotificationsRead() {
		notifications.markAllRead(current.require().getId());
		return true;
	}

	@MutationMapping
	public boolean markNotificationRead(@Argument Long id) {
		notifications.markRead(current.require(), id);
		return true;
	}

	@MutationMapping
	public boolean muteNotificationType(@Argument NotificationType type) {
		notifications.mute(current.require().getId(), type);
		return true;
	}

	@MutationMapping
	public boolean unmuteNotificationType(@Argument NotificationType type) {
		notifications.unmute(current.require().getId(), type);
		return true;
	}
}
