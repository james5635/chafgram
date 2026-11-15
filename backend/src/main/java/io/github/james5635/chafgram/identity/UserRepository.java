package io.github.james5635.chafgram.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	@Query(nativeQuery = true, value = "SELECT COUNT(*) FROM users WHERE role = 'admin' AND status = 'active'")
	long countActiveAdmins();
}
