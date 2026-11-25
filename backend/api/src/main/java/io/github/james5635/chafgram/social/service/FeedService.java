package io.github.james5635.chafgram.social.service;

import io.github.james5635.chafgram.identity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import io.github.james5635.chafgram.social.repository.PostRepository;
import io.github.james5635.chafgram.social.repository.UserBlockRepository;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.PostType;

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
		// BR-2.2: blocking is mutual-effect — neither side sees the other
		List<Long> authors = followees.stream()
				.filter(id -> !isBlockedAuthor(actor.getId(), id))
				.distinct()
				.collect(Collectors.toList());
		// the following feed shows your own posts too — otherwise nobody sees what they write
		authors.add(actor.getId());
		return posts.findFeed(authors, cursor, page(first));
	}

	@Transactional(readOnly = true)
	public List<Post> discoverFeed(User actor, int first, long cursor) {
		int limit = Math.max(first, 1);
		List<Post> allowed = new ArrayList<>();
		long currentCursor = cursor;
		while (allowed.size() < limit) {
			List<Post> page = posts.findDiscover(currentCursor, page(limit));
			if (page.isEmpty()) {
				break;
			}
			currentCursor = page.get(page.size() - 1).getId();
			for (Post post : page) {
				if (allowed.size() >= limit) {
					break;
				}
				if (!isBlockedAuthor(actor.getId(), post.getAuthorId())) {
					allowed.add(post);
				}
			}
		}
		return allowed;
	}

	/**
	 * Watch feed: short ready videos, newest first.
	 * <p>
	 * Rows the mutual-block filter hides are fetched past, not skipped: a
	 * blocked video must not leave the page short, and the next cursor must
	 * not stop on a hidden row — the older allowed videos behind it would
	 * become unreachable. Each iteration advances the cursor to the last row
	 * of the page fetched, so the loop ends when the query returns a page.
	 */
	@Transactional(readOnly = true)
	public List<Post> watchFeed(User actor, int first, long cursor) {
		int limit = Math.max(first, 1);
		List<Post> allowed = new ArrayList<>();
		long currentCursor = cursor;
		while (allowed.size() < limit) {
			List<Post> page = posts.findWatch(currentCursor, page(limit));
			if (page.isEmpty()) {
				break;
			}
			currentCursor = page.get(page.size() - 1).getId();
			for (Post p : page) {
				if (allowed.size() >= limit) {
					break;
				}
				if (!isBlockedAuthor(actor.getId(), p.getAuthorId())) {
					allowed.add(p);
				}
			}
		}
		return allowed;
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
		return Arrays.stream(sanitized.split("\s+"))
				.filter(t -> !t.isEmpty())
				.map(t -> "+" + t)
				.collect(Collectors.joining(" "));
	}
}
