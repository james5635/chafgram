package io.github.james5635.chafgram;

import org.springframework.beans.factory.annotation.Autowired;
import io.github.james5635.chafgram.media.TestMediaStorageConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base class for integration tests. Each test JVM gets a throwaway MySQL 8
 * container (Testcontainers) — hermetic and isolated from the dev database:
 * {@code just dev} data is never touched, and no {@code just seed} is needed
 * afterwards. Flyway migrates the container schema on context startup.
 *
 * <p>Requires Docker. The {@code mysql:8.0} image is pulled once and cached
 * locally. {@code wipeSocial()} clears the container's tables between test
 * classes/methods.</p>
 *
 * <p>The {@code test} profile activates {@code application-test.properties}
 * (test-only overrides). The main {@code application.properties} is loaded
 * as usual — a test-scoped file with that exact name would shadow it on the
 * classpath and silently drop the real configuration.</p>
 */
@SpringBootTest
@Import(TestMediaStorageConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

	protected static final MySQLContainer MYSQL =
			new MySQLContainer("mysql:8.0")
					.withDatabaseName("chafgram_test")
					.withUsername("test")
					.withPassword("test");

	static {
		MYSQL.start();
		// System properties outrank application.properties, so the container
		// datasource wins for every test class in the suite.
		System.setProperty("spring.datasource.url",
				MYSQL.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
		System.setProperty("spring.datasource.username", MYSQL.getUsername());
		System.setProperty("spring.datasource.password", MYSQL.getPassword());
	}

	@Autowired
	protected JdbcTemplate jdbc;

	/** Wipes every domain table (FK-safe order). */
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
}
