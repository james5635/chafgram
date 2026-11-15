package io.github.james5635.chafgram.moderation;

import io.github.james5635.chafgram.common.AuditLog;
import io.github.james5635.chafgram.common.AuditService;
import io.github.james5635.chafgram.identity.MemberAccess;
import io.github.james5635.chafgram.moderation.dto.AuditLogEntryDto;
import io.github.james5635.chafgram.moderation.dto.ReportDto;
import io.github.james5635.chafgram.social.CurrentMember;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

/** Staff-only moderation surface (moderator or admin). */
@Controller
@PreAuthorize("isFullyAuthenticated()")
public class ModerationQueryResolver {

	private final ReportService reports;
	private final AuditService audit;
	private final MemberAccess access;
	private final CurrentMember current;

	public ModerationQueryResolver(ReportService reports, AuditService audit, MemberAccess access, CurrentMember current) {
		this.reports = reports;
		this.audit = audit;
		this.access = access;
		this.current = current;
	}

	@QueryMapping
	public List<ReportDto> reportQueue(@Argument Integer first) {
		var actor = access.requireStaff(current.require().getUsername());
		int limit = first == null ? 100 : Math.min(Math.max(first, 1), 100);
		return reports.openQueue(actor).stream().limit(limit).map(ModerationQueryResolver::toDto).toList();
	}

	@QueryMapping
	public List<AuditLogEntryDto> auditLog(@Argument Integer first) {
		access.requireStaff(current.require().getUsername());
		int limit = first == null ? 100 : Math.min(Math.max(first, 1), 100);
		return audit.recent(limit).stream().map(ModerationQueryResolver::toDto).toList();
	}

	private static ReportDto toDto(Report r) {
		Long reporterId = r.getReporterId() == null ? -1L : r.getReporterId();
		Long resolvedBy = r.getResolvedBy() == null ? null : r.getResolvedBy();
		return new ReportDto(r.getId(), reporterId, r.getTargetType().name().toLowerCase(),
				r.getTargetId(), r.getReason().name().toLowerCase(),
				r.getStatus().name().toLowerCase(), resolvedBy, r.getResolvedAt(), r.getCreatedAt());
	}

	private static AuditLogEntryDto toDto(AuditLog a) {
		Long actorId = a.getActorId() == null ? null : a.getActorId();
		return new AuditLogEntryDto(a.getId(), actorId, a.getAction(),
				a.getTargetType() == null ? "" : a.getTargetType(), a.getTargetId(), a.getCreatedAt());
	}
}
