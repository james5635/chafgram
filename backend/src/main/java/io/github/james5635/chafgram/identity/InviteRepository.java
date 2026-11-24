package io.github.james5635.chafgram.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Long> {

	Optional<Invite> findByTokenHash(String tokenHash);

	List<Invite> findAllByOrderByCreatedAtDesc();
}
