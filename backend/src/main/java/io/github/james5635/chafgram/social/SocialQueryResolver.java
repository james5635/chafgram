package io.github.james5635.chafgram.social;
import io.github.james5635.chafgram.social.dto.Views;
import io.github.james5635.chafgram.social.dto.MemberDto;
import io.github.james5635.chafgram.social.dto.PostDto;
import io.github.james5635.chafgram.social.dto.CommentDto;
import io.github.james5635.chafgram.social.dto.NotificationDto;
import io.github.james5635.chafgram.social.dto.ReactionSummaryDto;
import io.github.james5635.chafgram.social.dto.FeedPageDto;
import io.github.james5635.chafgram.social.CurrentMember;

import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserRepository;
import io.github.james5635.chafgram.social.CommentService;
import io.github.james5635.chafgram.social.FeedService;
import io.github.james5635.chafgram.social.FollowService;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.PostService;
import io.github.james5635.chafgram.social.UserBlockRepository;
import io.github.james5635.chafgram.notifications.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import io.github.james5635.chafgram.common.ApiException;

import java.util.List;

@Controller
@PreAuthorize("isFullyAuthenticated()")
public class SocialQueryResolver {

	private final CurrentMember current;
	private final Views views;
	private final PostService posts;
	private final FeedService feed;
	private final CommentService comments;
	private final FollowService follows;
	private final UserBlockRepository blocks;
	private final NotificationService notifications;
	private final UserRepository users;

	public SocialQueryResolver(CurrentMember current, Views views, PostService posts, FeedService feed,
			CommentService comments, FollowService follows, UserBlockRepository blocks,
			NotificationService notifications, UserRepository users) {
		this.current = current;
		this.views = views;
		this.posts = posts;
		this.feed = feed;
		this.comments = comments;
		this.follows = follows;
		this.blocks = blocks;
		this.notifications = notifications;
		this.users = users;
	}

	@QueryMapping
	public MemberDto me() {
		User actor = current.require();
		return views.member(actor, actor);
	}

	@QueryMapping
	public MemberDto member(@Argument String username) {
		User viewer = current.require();
		User target = findByUsername(username);
		return views.member(target, viewer);
	}

	@QueryMapping
	public List<MemberDto> followers(@Argument String username, @Argument int first) {
		User viewer = current.require();
		User target = findByUsername(username);
		return follows.followers(target.getId(), PageRequest.of(0, Math.min(Math.max(first, 1), 100))).stream()
				.map(u -> views.member(u, viewer))
				.toList();
	}

	@QueryMapping
	public List<MemberDto> following(@Argument String username, @Argument int first) {
		User viewer = current.require();
		User target = findByUsername(username);
		return follows.followees(target.getId(), PageRequest.of(0, Math.min(Math.max(first, 1), 100))).stream()
				.map(u -> views.member(u, viewer))
				.toList();
	}

	@QueryMapping
	public FeedPageDto followingFeed(@Argument Integer first, @Argument String cursor) {
		User actor = current.require();
		int limit = first == null ? 20 : Math.min(Math.max(first, 1), 100);
		long afterId = views.decodeCursor(cursor);
		List<Post> page = feed.followingFeed(actor, limit, afterId);
		return new FeedPageDto(views.posts(page, actor), views.nextCursor(page, cursor));
	}

	@QueryMapping
	public FeedPageDto discoverFeed(@Argument Integer first, @Argument String cursor) {
		User actor = current.require();
		int limit = first == null ? 20 : Math.min(Math.max(first, 1), 100);
		long afterId = views.decodeCursor(cursor);
		List<Post> page = feed.discoverFeed(actor, limit, afterId);
		return new FeedPageDto(views.posts(page, actor), views.nextCursor(page, cursor));
	}

	@QueryMapping
	public FeedPageDto watchFeed(@Argument Integer first, @Argument String cursor) {
		User actor = current.require();
		int limit = first == null ? 20 : Math.min(Math.max(first, 1), 100);
		long afterId = views.decodeCursor(cursor);
		List<Post> page = feed.watchFeed(actor, limit, afterId);
		return new FeedPageDto(views.posts(page, actor), views.nextCursor(page, cursor));
	}

	@QueryMapping
	public PostDto post(@Argument Long id) {
		User actor = current.require();
		Post post = posts.get(id);
		return views.post(post, actor);
	}

	@QueryMapping
	public List<CommentDto> postComments(@Argument Long postId, @Argument Integer first) {
		User actor = current.require();
		int limit = first == null ? 50 : Math.min(Math.max(first, 1), 200);
		return views.comments(comments.listByPost(postId, limit), actor);
	}

	@QueryMapping
	public List<PostDto> searchPosts(@Argument String query, @Argument Integer first, @Argument PostType kind) {
		User actor = current.require();
		int limit = first == null ? 20 : Math.min(Math.max(first, 1), 100);
		return views.posts(feed.search(actor, query, limit, kind), actor);
	}

	@QueryMapping
	public List<PostDto> savedPosts(@Argument Integer first) {
		User actor = current.require();
		int limit = first == null ? 20 : Math.min(Math.max(first, 1), 100);
		return views.posts(posts.savedBy(actor.getId(), limit), actor);
	}

	@QueryMapping
	public List<NotificationDto> myNotifications(@Argument Integer first) {
		User actor = current.require();
		int limit = first == null ? 30 : Math.min(Math.max(first, 1), 100);
		return views.notifications(notifications.listVisible(actor, limit), actor);
	}

	@QueryMapping
	public long unreadNotifications() {
		return notifications.countUnreadVisible(current.require());
	}

	@QueryMapping
	public List<MemberDto> blockedMembers() {
		User actor = current.require();
		return blocks.findBlockedIds(actor.getId()).stream()
				.map(id -> views.member(views.findUser(id), actor))
				.toList();
	}

	private User findByUsername(String username) {
		return users.findByUsername(username)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found"));
	}
}
