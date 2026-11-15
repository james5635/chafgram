package io.github.james5635.chafgram.notifications;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The two-column `notification_preferences` mute table is deliberately dumb
 * (presence = muted). No entity is mapped — Hibernate forbids converters on
 * @Id attributes, so this uses plain SQL.
 */
@Repository
public class NotificationMuteStore {

	private final JdbcTemplate jdbc;

	public NotificationMuteStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public boolean isMuted(Long userId, NotificationType type) {
		Integer count = jdbc.queryForObject(
				"SELECT COUNT(*) FROM notification_preferences WHERE user_id = ? AND type = ?",
				Integer.class, userId, type.name().toLowerCase());
		return count != null && count > 0;
	}

	public void mute(Long userId, NotificationType type) {
		jdbc.update("INSERT IGNORE INTO notification_preferences (user_id, type) VALUES (?, ?)",
				userId, type.name().toLowerCase());
	}

	public void unmute(Long userId, NotificationType type) {
		jdbc.update("DELETE FROM notification_preferences WHERE user_id = ? AND type = ?",
				userId, type.name().toLowerCase());
	}
}
