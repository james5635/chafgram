package io.github.james5635.chafgram.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import io.github.james5635.chafgram.identity.AccountToken;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {

	@Query("select t from AccountToken t where t.tokenHash = :hash")
	Optional<AccountToken> findByHash(@Param("hash") String hash);

	/**
	 * Consumes a token in one atomic statement: only the first caller sees
	 * {@code usedAt IS NULL}, so a token can never be redeemed twice, even
	 * when two requests race.
	 *
	 * @return the number of rows consumed — 1 for the winner, 0 for everyone else
	 */
	@Modifying
	@Transactional
	@Query("UPDATE AccountToken t SET t.usedAt = :now WHERE t.id = :id AND t.usedAt IS NULL AND t.expiresAt > :now")
	int consumeIfUsable(@Param("id") Long id, @Param("now") Instant now);
}
