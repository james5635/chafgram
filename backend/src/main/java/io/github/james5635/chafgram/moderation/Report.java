package io.github.james5635.chafgram.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class Report {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "reporter_id")
	private Long reporterId;

	@Convert(converter = ReportTargetTypeConverter.class)
	@Column(name = "target_type", nullable = false, length = 7)
	private ReportTargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Convert(converter = ReportReasonConverter.class)
	@Column(nullable = false, length = 13)
	private ReportReason reason;

	@Column(columnDefinition = "TEXT")
	private String details;

	@Convert(converter = ReportStatusConverter.class)
	@Column(name = "status", nullable = false, length = 9)
	private ReportStatus status = ReportStatus.OPEN;

	@Column(name = "resolved_by")
	private Long resolvedBy;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
