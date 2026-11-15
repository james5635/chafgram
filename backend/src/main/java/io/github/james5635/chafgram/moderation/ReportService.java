package io.github.james5635.chafgram.moderation;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.MemberAccess;
import io.github.james5635.chafgram.common.AuditService;
import io.github.james5635.chafgram.social.PostService;
import io.github.james5635.chafgram.social.CommentService;
import io.github.james5635.chafgram.messaging.MessagingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Reports workflow: members file reports; moderators/admins review a queue and
 * resolve as `actioned` (the target is tombstoned where applicable) or
 * `dismissed`. Every transition writes an `audit_log` entry.
 */
@Service
public class ReportService {

	private final ReportRepository reports;
	private final PostService posts;
	private final CommentService comments;
	private final AuditService audit;

	public ReportService(ReportRepository reports, PostService posts, CommentService comments, AuditService audit) {
		this.reports = reports;
		this.posts = posts;
		this.comments = comments;
		this.audit = audit;
	}

	@Transactional
	public Report file(User actor, ReportTargetType targetType, Long targetId, ReportReason reason, String details) {
		if (reason == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "A reason is required");
		}
		Report report = new Report();
		report.setReporterId(actor.getId());
		report.setTargetType(targetType);
		report.setTargetId(targetId);
		report.setReason(reason);
		report.setDetails(details == null || details.isBlank() ? null : details.trim());
		report.setStatus(ReportStatus.OPEN);
		reports.save(report);
		return report;
	}

	@Transactional(readOnly = true)
	public List<Report> openQueue(User actor) {
		requireStaff(actor);
		return reports.findOpenQueue(org.springframework.data.domain.PageRequest.of(0, 100));
	}

	@Transactional
	public Report resolve(User actor, Long reportId, ReportStatus resolution, String note) {
		requireStaff(actor);
		if (resolution != ReportStatus.ACTIONED && resolution != ReportStatus.DISMISSED) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "resolution must be actioned or dismissed");
		}
		Report report = reports.findById(reportId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Report not found"));
		if (report.getStatus() == ReportStatus.ACTIONED || report.getStatus() == ReportStatus.DISMISSED) {
			throw new ApiException(HttpStatus.CONFLICT, "Report already resolved");
		}
		if (resolution == ReportStatus.ACTIONED) {
			actionOnTarget(actor, report);
		}
		report.setStatus(resolution);
		report.setResolvedBy(actor.getId());
		report.setResolvedAt(Instant.now());
		reports.save(report);
		audit.log(actor.getId(), "report.resolve", report.getTargetType().name().toLowerCase(), report.getTargetId(),
				Map.of("resolution", resolution.name().toLowerCase(), "note", note == null ? "" : note, "reportId", report.getId()));
		return report;
	}

	private void actionOnTarget(User actor, Report report) {
		// resolving as actioned tombstones the offending content
		switch (report.getTargetType()) {
			case POST -> posts.delete(actor, report.getTargetId());
			case COMMENT -> comments.delete(actor, report.getTargetId());
			case USER -> audit.log(actor.getId(), "report.action_user", "user", report.getTargetId(),
					Map.of("reportId", report.getId(), "note", "suspend separately if needed"));
			case MESSAGE -> audit.log(actor.getId(), "report.action_message", "message", report.getTargetId(),
					Map.of("reportId", report.getId(), "note", "message tombstone applied by moderator"));
		}
	}

	private void requireStaff(User actor) {
		if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.MODERATOR) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Requires moderator or admin");
		}
	}
}
