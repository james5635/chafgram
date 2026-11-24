package io.github.james5635.chafgram.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	@Query(nativeQuery = true, value = "SELECT COUNT(*) FROM users WHERE role = 'admin' AND status = 'active'")
	long countActiveAdmins();

	/** Throttled last-seen write: only when never seen or the throttle window has passed. */
	@Modifying
	@Query("UPDATE User u SET u.lastSeenAt = :now WHERE u.id = :id "
			+ "AND (u.lastSeenAt IS NULL OR u.lastSeenAt < :cutoff)")
	int touchLastSeen(@Param("id") Long id, @Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
