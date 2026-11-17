package io.github.james5635.chafgram.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Append-only moderation/admin trail. Nothing ever edits or deletes these rows. */
@Service
public class AuditService {

	private final AuditLogRepository auditLog;

	public AuditService(AuditLogRepository auditLog) {
		this.auditLog = auditLog;
	}

	@Transactional
	public void log(Long actorId, String action, String targetType, Long targetId, Map<String, Object> metadata) {
		AuditLog entry = new AuditLog();
		entry.setActorId(actorId);
		entry.setAction(action);
		entry.setTargetType(targetType);
		entry.setTargetId(targetId);
		entry.setMetadata(metadata);
		auditLog.save(entry);
	}

	@Transactional(readOnly = true)
	public List<AuditLog> recent(int first) {
		return auditLog.findRecent(PageRequest.of(0, Math.min(Math.max(first, 1), 100)));
	}
}
