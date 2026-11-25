package io.github.james5635.chafgram.social.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.common.service.AuditService;
import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.media.MediaAsset;
import io.github.james5635.chafgram.media.repository.MediaAssetRepository;
import io.github.james5635.chafgram.media.MediaKind;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import io.github.james5635.chafgram.social.repository.PostMediaRepository;
import io.github.james5635.chafgram.social.repository.PostRepository;
import io.github.james5635.chafgram.social.repository.SavedPostRepository;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.PostMedia;
import io.github.james5635.chafgram.social.PostType;
import io.github.james5635.chafgram.social.SavedPost;
import io.github.james5635.chafgram.social.SavedPostId;

/** Posts: create/update/tombstone delete, listings, and media checks (BR-3.x, BR-5.x). */
@Service
public class PostService {

	static final int MAX_BODY = 5000;

	private final PostRepository posts;
	private final PostMediaRepository postMedia;
	private final MediaAssetRepository mediaAssets;
	private final UserRepository users;
	private final SavedPostRepository savedPosts;
	private final AuditService audit;
	private final MemberAccess access;

	public PostService(PostRepository posts, PostMediaRepository postMedia,
			MediaAssetRepository mediaAssets, UserRepository users, SavedPostRepository savedPosts,
			AuditService audit, MemberAccess access) {
		this.posts = posts;
		this.access = access;
		this.postMedia = postMedia;
		this.mediaAssets = mediaAssets;
		this.users = users;
		this.savedPosts = savedPosts;
		this.audit = audit;
	}

	@Transactional
	public Post create(User actor, PostType type, String body, List<Long> mediaIds) {
		access.requireWriteable(actor);
		validate(actor, type, body, mediaIds);

		Post post = new Post();
		post.setAuthorId(actor.getId());
		post.setType(type);
		post.setBody(body == null || body.isBlank() ? null : body.trim());
		posts.save(post);

		attachMedia(post.getId(), mediaIds);
		return post;
	}

	@Transactional
	public Post update(User actor, Long postId, String body) {
		access.requireWriteable(actor);
		Post post = findVisible(postId);
		if (!post.getAuthorId().equals(actor.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Only the author can edit this post"); // BR-3.5
		}
		if (post.getType() == PostType.NOTE && (body == null || body.isBlank())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "A note needs text");
		}
		validate(actor, null, body, List.of());
		post.setBody(body == null || body.isBlank() ? null : body.trim());
		return posts.save(post);
	}

	@Transactional
	public void delete(User actor, Long postId) {
		access.requireWriteable(actor);
		Post post = posts.findById(postId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found"));
		boolean isAuthor = post.getAuthorId().equals(actor.getId());
		boolean isStaff = actor.getRole() == Role.ADMIN || actor.getRole() == Role.MODERATOR;
		if (!isAuthor && !isStaff) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Only the author or a moderator can delete this post");
		}
		if (post.getDeletedAt() == null) {
			post.setDeletedAt(Instant.now()); // tombstone, not a row delete (BR-3.3 spirit)
			posts.save(post);
			if (isStaff && !isAuthor) {
				audit.log(actor.getId(), "post.delete", "post", post.getId(),
						Map.of("authorId", post.getAuthorId(), "staffAction", true));
			}
		}
	}

	@Transactional(readOnly = true)
	public Post get(Long postId) {
		return findVisible(postId);
	}

	@Transactional(readOnly = true)
	public List<Post> listByAuthor(Long authorId, int limit) {
		return posts.findByAuthor(authorId, PageRequest.of(0, limit));
	}

	@Transactional(readOnly = true)
	public List<Post> savedBy(Long userId, int limit) {
		return posts.findAllById(savedPosts.findPostIdsByUser(userId)).stream()
				.filter(p -> p.getDeletedAt() == null)
				.limit(limit)
				.toList();
	}

	public boolean isSaved(Long userId, Long postId) {
		return savedPosts.isSaved(userId, postId);
	}

	@Transactional
	public void save(User actor, Long postId) {
		findVisible(postId);
		if (!savedPosts.existsById(new SavedPostId(actor.getId(), postId))) {
			SavedPost saved = new SavedPost();
			saved.setUserId(actor.getId());
			saved.setPostId(postId);
			savedPosts.save(saved);
		}
	}

	@Transactional
	public void unsave(User actor, Long postId) {
		savedPosts.deleteById(new SavedPostId(actor.getId(), postId));
	}

	Post findVisible(Long postId) {
		return posts.findVisibleById(postId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found or removed"));
	}

	private void attachMedia(Long postId, List<Long> mediaIds) {
		if (mediaIds == null) {
			return;
		}
		short position = 0;
		for (Long mediaId : mediaIds) {
			PostMedia link = new PostMedia();
			link.setPostId(postId);
			link.setMediaId(mediaId);
			link.setPosition(position++);
			postMedia.save(link);
		}
	}

	private void validate(User actor, PostType requiredType, String body, List<Long> mediaIds) {
		if (body != null && body.length() > MAX_BODY) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Post body is limited to " + MAX_BODY + " characters");
		}
		if (requiredType == null) {
			return; // update path: only body validation
		}
		// create path: type-specific rules (BR-3.2)
		List<Long> media = mediaIds == null ? List.of() : mediaIds;
		if (requiredType == PostType.NOTE) {
			if (media.isEmpty() && (body == null || body.isBlank())) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "A note needs text or media");
			}
			for (Long id : media) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Notes cannot carry media");
			}
		}
		if (media.isEmpty()) {
			if (requiredType == PostType.PHOTO || requiredType == PostType.VIDEO) {
				throw new ApiException(HttpStatus.BAD_REQUEST, requiredType.name().toLowerCase() + " posts require at least one media asset");
			}
			return;
		}
		// media kind must match the post type, and every asset must be owned by the author (BR-5.3)
		for (Long id : media) {
			MediaAsset asset = mediaAssets.findById(id)
					.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Media not found: " + id));
			if (!asset.getOwnerUserId().equals(actor.getId())) {
				throw new ApiException(HttpStatus.FORBIDDEN, "You can only attach media you uploaded");
			}
			if (requiredType == PostType.PHOTO && asset.getKind() != MediaKind.IMAGE) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Photo posts take images only");
			}
			if (requiredType == PostType.VIDEO && asset.getKind() != MediaKind.VIDEO) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Video posts take one video only");
			}
		}
		if (requiredType == PostType.VIDEO && media.size() != 1) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Video posts take exactly one video");
		}
	}
}
