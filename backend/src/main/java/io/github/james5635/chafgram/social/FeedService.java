package io.github.james5635.chafgram.social;

import io.github.james5635.chafgram.identity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Pull-based feeds, no fan-out materialization: following feed = posts from
 * followed authors ∪ own posts, sorted by id (monotonic with created_at);
 * discover = everything visible; watch = short videos only; both exclude blocked authors.
 */
@Service
public class FeedService {

	private final PostRepository posts;
	private final UserBlockRepository blocks;
	private final FollowService follows;

	public FeedService(PostRepository posts, UserBlockRepository blocks, FollowService follows) {
		this.posts = posts;
		this.blocks = blocks;
		this.follows = follows;
	}

	@Transactional(readOnly = true)
	public List<Post> followingFeed(User actor, int first, long cursor) {
		List<Long> followees = follows.followeeIds(actor.getId());
		List<Long> blocked = blockedPlus(actor.getId());
		List<Long> authors = followees.stream().filter(id -> !blocked.contains(id)).distinct().collect(java.util.stream.Collectors.toList());
		// the following feed shows your own posts too — otherwise nobody sees what they write
		authors.add(actor.getId());
		return posts.findFeed(authors, cursor, page(first));
	}

	@Transactional(readOnly = true)
	public List<Post> discoverFeed(User actor, int first, long cursor) {
		return posts.findDiscover(cursor, page(first)).stream()
				.filter(p -> !isBlockedAuthor(actor.getId(), p.getAuthorId()))
				.limit(Math.max(first, 1))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Post> watchFeed(User actor, int first, long cursor) {
		List<Long> blocked = blockedPlus(actor.getId());
		return posts.findWatch(cursor, page(first)).stream()
				.filter(p -> !blocked.contains(p.getAuthorId()))
				.limit(Math.max(first, 1))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Post> search(User actor, String rawQuery, int first) {
		String booleanQuery = toBooleanQuery(rawQuery);
		if (booleanQuery.isEmpty()) {
			return List.of();
		}
		return posts.searchIds(booleanQuery, PageRequest.of(0, Math.min(Math.max(first, 1), 100)))
				.stream()
				.map(id -> posts.findVisibleById(id).orElse(null))
				.filter(p -> p != null && !isBlockedAuthor(actor.getId(), p.getAuthorId()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Post> search(User actor, String rawQuery, int first, PostType kind) {
		String booleanQuery = toBooleanQuery(rawQuery);
		if (booleanQuery.isEmpty()) {
			return List.of();
		}
		return posts.searchIdsWithKind(booleanQuery, kind == null ? null : kind.name(), PageRequest.of(0, Math.min(Math.max(first, 1), 100)))
				.stream()
				.map(id -> posts.findVisibleById(id).orElse(null))
				.filter(p -> p != null && !isBlockedAuthor(actor.getId(), p.getAuthorId()))
				.toList();
	}

	@Transactional(readOnly = true)
	public boolean isBlockedAuthor(Long viewerId, Long authorId) {
		return blocks.isBlockedBy(viewerId, authorId) || blocks.isBlockedBy(authorId, viewerId);
	}

	private List<Long> blockedPlus(Long userId) {
		// users who blocked ME or whom I blocked
		List<Long> mine = new java.util.ArrayList<>(blocks.findBlockedIds(userId));
		// note: reverse direction is checked per-author in isBlockedAuthor; this covers my blocks only
		return mine;
	}

	private PageRequest page(int first) {
		return PageRequest.of(0, Math.min(Math.max(first, 1), 100));
	}

	/** Strip anything that isn't a word character, then require every term (+term). */
	static String toBooleanQuery(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		String sanitized = raw.replaceAll("[^\\p{L}\\p{N} ]", " ").trim();
		if (sanitized.isEmpty()) {
			return "";
		}
		return java.util.Arrays.stream(sanitized.split("\s+"))
				.filter(t -> !t.isEmpty())
				.map(t -> "+" + t)
				.collect(java.util.stream.Collectors.joining(" "));
	}
}
