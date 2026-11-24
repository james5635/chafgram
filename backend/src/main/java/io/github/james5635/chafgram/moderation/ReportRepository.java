package io.github.james5635.chafgram.moderation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

	@Query("select r from Report r where r.status = io.github.james5635.chafgram.moderation.ReportStatus.OPEN order by r.id asc")
	List<Report> findOpenQueue(Pageable pageable);
}
