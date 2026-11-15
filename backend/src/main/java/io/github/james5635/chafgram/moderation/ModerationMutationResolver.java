package io.github.james5635.chafgram.moderation;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.MemberAccess;
import io.github.james5635.chafgram.moderation.dto.ReportDto;
import io.github.james5635.chafgram.social.CurrentMember;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.Locale;

/** Report filing is any active member; resolution is staff only. */
@Controller
@PreAuthorize("isFullyAuthenticated()")
public class ModerationMutationResolver {

	private final ReportService reports;
	private final MemberAccess access;
	private final CurrentMember current;

	public ModerationMutationResolver(ReportService reports, MemberAccess access, CurrentMember current) {
		this.reports = reports;
		this.access = access;
		this.current = current;
	}

	@MutationMapping
	public ReportDto fileReport(@Argument String targetType, @Argument Long targetId,
			@Argument String reason, @Argument String details) {
		var actor = access.requireWriteable(current.require().getUsername());
		if (targetId == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "targetId is required");
		}
		Report report = reports.file(
				actor,
				parseEnum(ReportTargetType.class, targetType, "targetType"),
				targetId,
				parseEnum(ReportReason.class, reason, "reason"),
				details);
		return toDto(report);
	}

	@MutationMapping
	public ReportDto resolveReport(@Argument Long id, @Argument String resolution, @Argument String note) {
		var actor = access.requireStaff(current.require().getUsername());
		ReportStatus resolutionStatus = parseEnum(ReportStatus.class, resolution, "resolution");
		Report report = reports.resolve(actor, id, resolutionStatus, note);
		return toDto(report);
	}

	private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String field) {
		try {
			return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
		}
		catch (Exception e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, field + " is invalid");
		}
	}

	private static ReportDto toDto(Report r) {
		Long reporterId = r.getReporterId() == null ? -1L : r.getReporterId();
		Long resolvedBy = r.getResolvedBy() == null ? null : r.getResolvedBy();
		return new ReportDto(r.getId(), reporterId, r.getTargetType().name().toLowerCase(),
				r.getTargetId(), r.getReason().name().toLowerCase(),
				r.getStatus().name().toLowerCase(), resolvedBy, r.getResolvedAt(), r.getCreatedAt());
	}
}
