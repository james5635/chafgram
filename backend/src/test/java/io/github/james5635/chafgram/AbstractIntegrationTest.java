package io.github.james5635.chafgram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Base class for integration tests. Tests run against the dev MySQL from
 * {@code compose.yaml} — start it with `just mysql`.
 *
 * <p><b>WARNING:</b> the suite wipes and re-creates the tables on every run.
 * It points at the dev {@code chafgram} schema; run {@code just seed} afterwards
 * to restore the dev fixtures. Point {@code CHAFGRAM_TEST_DATASOURCE_URL} at a
 * different schema if you don't want your dev data disturbed.</p>
 *
 * <p>No Testcontainers: the dev database doubles as the test database, and the
 * schema is migrated by Flyway like any other.</p>
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

	@Autowired
	protected JdbcTemplate jdbc;

	/** Wipes every domain table (FK-safe order). Tests run against a dedicated, disposable schema. */
	protected void wipeSocial() {
		String[] tables = {
				"audit_log", "reports", "notifications", "notification_preferences",
				"message_media", "messages", "conversation_members", "conversations",
				"saved_posts", "post_reactions", "comment_reactions", "comments", "post_media", "posts",
				"media_assets", "user_blocks", "follows", "invites", "users",
		};
		for (String table : tables) {
			jdbc.update("DELETE FROM " + table);
		}
	}

	static {
		// Fail fast at class-load time with a clear message, before Spring's
		// context (and Flyway) even tries to connect.
		String url = System.getenv().getOrDefault(
				"CHAFGRAM_TEST_DATASOURCE_URL",
				"jdbc:mysql://localhost:3306/chafgram?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=2000");
		String user = System.getenv().getOrDefault("CHAFGRAM_TEST_DATASOURCE_USERNAME", "chafgram");
		String password = System.getenv().getOrDefault("CHAFGRAM_TEST_DATASOURCE_PASSWORD", "chafgram");
		try (Connection ignored = DriverManager.getConnection(url, user, password)) {
			// reachable — Flyway will migrate the schema during context startup
		}
		catch (SQLException e) {
			throw new ExceptionInInitializerError(new IllegalStateException(
					"Cannot reach the integration test database at " + url
							+ ". Run `just mysql` first. Override the connection with"
							+ " CHAFGRAM_TEST_DATASOURCE_URL / _USERNAME / _PASSWORD. Cause: " + e.getMessage(),
					e));
		}
	}
}
