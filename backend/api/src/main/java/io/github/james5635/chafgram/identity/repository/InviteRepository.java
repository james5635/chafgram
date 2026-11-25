package io.github.james5635.chafgram.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import io.github.james5635.chafgram.identity.Invite;

public interface InviteRepository extends JpaRepository<Invite, Long> {

	Optional<Invite> findByTokenHash(String tokenHash);

	List<Invite> findAllByOrderByCreatedAtDesc();
}
