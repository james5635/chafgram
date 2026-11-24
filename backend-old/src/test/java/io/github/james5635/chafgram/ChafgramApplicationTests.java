package io.github.james5635.chafgram;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ChafgramApplicationTests extends AbstractIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void contextLoadsAndFlywaySeedsTheWorkspace() {
		Long settingsRows = jdbc.queryForObject(
				"SELECT COUNT(*) FROM workspace_settings", Long.class);
		assertThat(settingsRows).isEqualTo(1L);

		Long tableCount = jdbc.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
				Long.class);
		assertThat(tableCount).isGreaterThanOrEqualTo(23L);
	}
}
