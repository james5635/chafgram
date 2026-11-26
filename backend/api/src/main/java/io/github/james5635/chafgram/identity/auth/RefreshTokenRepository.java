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
}
