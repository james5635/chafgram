package io.github.james5635.chafgram.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {

	@Query("select t from AccountToken t where t.tokenHash = :hash")
	Optional<AccountToken> findByHash(@Param("hash") String hash);
}
