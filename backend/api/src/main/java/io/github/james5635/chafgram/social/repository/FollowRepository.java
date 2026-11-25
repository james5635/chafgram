package io.github.james5635.chafgram.social.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.james5635.chafgram.identity.User;
import java.util.List;
import io.github.james5635.chafgram.social.Follow;
import io.github.james5635.chafgram.social.FollowId;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

	@Query("select count(f) from Follow f where f.followerId = :id")
	long countFollowing(@Param("id") Long id);

	@Query("select count(f) from Follow f where f.followeeId = :id")
	long countFollowers(@Param("id") Long id);

	@Query("select f.followeeId from Follow f where f.followerId = :id")
	List<Long> findFolloweeIds(@Param("id") Long id);

	@Query("select case when count(f) > 0 then true else false end from Follow f where f.followerId = :followerId and f.followeeId = :followeeId")
	boolean isFollowing(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

	@Query("select u from User u join Follow f on f.followeeId = u.id where f.followerId = :id order by f.createdAt desc")
	List<User> findFollowees(@Param("id") Long id, Pageable pageable);

	@Query("select u from User u join Follow f on f.followerId = u.id where f.followeeId = :id order by f.createdAt desc")
	List<User> findFollowers(@Param("id") Long id, Pageable pageable);
}
