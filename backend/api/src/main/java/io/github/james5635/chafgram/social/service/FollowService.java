package io.github.james5635.chafgram.social.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.notifications.NotificationEntityType;
import io.github.james5635.chafgram.notifications.service.NotificationService;
import io.github.james5635.chafgram.notifications.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import io.github.james5635.chafgram.social.repository.FollowRepository;
import io.github.james5635.chafgram.social.repository.UserBlockRepository;
import io.github.james5635.chafgram.social.Follow;
import io.github.james5635.chafgram.social.FollowId;

/** Follows and the denormalized follow counts (never stored — derived, BR-2.3). */
@Service
public class FollowService {

	private final UserRepository users;
	private final FollowRepository follows;
	private final UserBlockRepository blocks;
	private final NotificationService notifications;

	public FollowService(UserRepository users, FollowRepository follows, UserBlockRepository blocks,
			NotificationService notifications) {
		this.users = users;
		this.follows = follows;
		this.blocks = blocks;
		this.notifications = notifications;
	}

	@Transactional
	public void follow(User actor, Long targetId) {
		User target = findOrThrow(targetId);
		if (target.getId().equals(actor.getId())) {
			throw new ApiException(HttpStatus.CONFLICT, "You cannot follow yourself"); // BR-2.1
		}
		// mutual block hides the relationship entirely
		if (blocks.isBlockedBy(actor.getId(), targetId) || blocks.isBlockedBy(targetId, actor.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cannot follow this member");
		}
		FollowId id = new FollowId(actor.getId(), targetId);
		if (follows.existsById(id)) {
			return;
		}
		Follow follow = new Follow();
		follow.setFollowerId(actor.getId());
		follow.setFolloweeId(targetId);
		follows.save(follow);
		notifications.notify(targetId, actor.getId(), NotificationType.FOLLOW, NotificationEntityType.USER, actor.getId());
	}

	@Transactional
	public void unfollow(User actor, Long targetId) {
		follows.deleteById(new FollowId(actor.getId(), targetId));
	}

	@Transactional(readOnly = true)
	public boolean isFollowing(Long followerId, Long followeeId) {
		return follows.isFollowing(followerId, followeeId);
	}

	@Transactional(readOnly = true)
	public long followingCount(Long userId) {
		return follows.countFollowing(userId);
	}

	@Transactional(readOnly = true)
	public long followerCount(Long userId) {
		return follows.countFollowers(userId);
	}

	@Transactional(readOnly = true)
	public List<Long> followeeIds(Long userId) {
		return follows.findFolloweeIds(userId);
	}

	@Transactional(readOnly = true)
	public List<User> followers(Long userId, Pageable pageable) {
		return follows.findFollowers(userId, pageable);
	}

	@Transactional(readOnly = true)
	public List<User> followees(Long userId, Pageable pageable) {
		return follows.findFollowees(userId, pageable);
	}

	private User findOrThrow(Long id) {
		return users.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found"));
	}
}
