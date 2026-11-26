package io.github.james5635.chafgram.identity.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

	/** Theft/logout revocation: no successor row is recorded, so reuse is detectable. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("UPDATE RefreshToken r SET r.revokedAt = :now "
			+ "WHERE r.userId = :userId AND r.revokedAt IS NULL")
	int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

	/**
	 * Claim a live row for rotation. The conditional UPDATE is the atomic
	 * compare-and-swap: exactly one concurrent refresher can claim a given hash
	 * because the second update sees {@code revoked_at} already set.
	 */
	@Modifying(flushAutomatically = true)
	@Transactional
	@Query("UPDATE RefreshToken r SET r.revokedAt = :now "
			+ "WHERE r.tokenHash = :tokenHash AND r.revokedAt IS NULL")
	int claimForRotation(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

	/** Record the successor on the row claimed by this transaction. */
	@Modifying(flushAutomatically = true)
	@Transactional
	@Query("UPDATE RefreshToken r SET r.replacedBy = :successorId "
			+ "WHERE r.tokenHash = :tokenHash AND r.replacedBy IS NULL")
	int recordReplacement(@Param("tokenHash") String tokenHash, @Param("successorId") Long successorId);
}
