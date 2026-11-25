package io.github.james5635.chafgram.social.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import io.github.james5635.chafgram.social.repository.UserBlockRepository;
import io.github.james5635.chafgram.social.UserBlock;
import io.github.james5635.chafgram.social.UserBlockId;

/** Mutual-effect blocks: neither party sees the other's content or messages. */
@Service
public class BlockService {

	private final UserRepository users;
	private final UserBlockRepository blocks;

	public BlockService(UserRepository users, UserBlockRepository blocks) {
		this.users = users;
		this.blocks = blocks;
	}

	@Transactional
	public void block(User actor, Long targetId) {
		if (actor.getId().equals(targetId)) {
			throw new ApiException(HttpStatus.CONFLICT, "You cannot block yourself");
		}
		findOrThrow(targetId);
		UserBlock block = new UserBlock();
		block.setBlockerId(actor.getId());
		block.setBlockedId(targetId);
		blocks.save(block);
	}

	@Transactional
	public void unblock(User actor, Long targetId) {
		blocks.deleteById(new UserBlockId(actor.getId(), targetId));
	}

	@Transactional(readOnly = true)
	public boolean blocks(Long blockerId, Long blockedId) {
		return blocks.isBlockedBy(blockerId, blockedId);
	}

	@Transactional(readOnly = true)
	public List<Long> blockedIds(Long userId) {
		return blocks.findBlockedIds(userId);
	}

	private User findOrThrow(Long id) {
		return users.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found"));
	}
}
