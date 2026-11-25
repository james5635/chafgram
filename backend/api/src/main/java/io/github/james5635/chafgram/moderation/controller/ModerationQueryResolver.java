package io.github.james5635.chafgram.moderation.controller;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.common.AuditLog;
import io.github.james5635.chafgram.common.service.AuditService;
import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.moderation.dto.AuditLogEntryDto;
import io.github.james5635.chafgram.moderation.dto.ReportDto;
import io.github.james5635.chafgram.social.utils.CurrentMember;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import io.github.james5635.chafgram.moderation.service.ReportService;
import io.github.james5635.chafgram.moderation.Report;

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

	/**
	 * Staff-only audit-log search (FR-10.2). All filters are optional:
	 * {@code actorId}, {@code action}, {@code targetType}, and the inclusive
	 * {@code from}/{@code to} ISO-8601 instants.
	 */
	@QueryMapping
	public List<AuditLogEntryDto> auditLog(@Argument Integer first,
			@Argument String actorId,
			@Argument String action,
			@Argument String targetType,
			@Argument String from,
			@Argument String to) {
		access.requireStaff(current.require().getUsername());
		int limit = first == null ? 100 : Math.min(Math.max(first, 1), 100);

		Long actorIdFilter = parseActorId(actorId);
		String actionFilter = blankToNull(action);
		String targetTypeFilter = blankToNull(targetType);
		Instant fromInstant = parseInstant(from, "from");
		Instant toInstant = parseInstant(to, "to");

		return audit.findByCriteria(actorIdFilter, actionFilter, targetTypeFilter,
				fromInstant, toInstant, limit)
				.stream().map(ModerationQueryResolver::toDto).toList();
	}

	private static Long parseActorId(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(raw.trim());
		}
		catch (NumberFormatException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid 'actorId' parameter: " + raw);
		}
	}

	private static Instant parseInstant(String raw, String field) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(raw.trim());
		}
		catch (DateTimeParseException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid '" + field + "' parameter: " + raw);
		}
	}

	private static String blankToNull(String raw) {
		return raw == null || raw.isBlank() ? null : raw.trim();
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
