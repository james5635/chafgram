package io.github.james5635.chafgram.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes the instance's app metadata on the admin-only
 * {@code /actuator/info} endpoint (FR-10.4).
 *
 * <p>Boot 4 does not contribute free-form {@code info.*} properties to the
 * info endpoint any more, so Chafgram contributes its own payload: the app
 * name and description, read from configuration.</p>
 */
@Component
public class AppInfoContributor implements InfoContributor {

	private final String name;
	private final String description;

	public AppInfoContributor(@Value("${info.app.name:Chafgram}") String name,
			@Value("${info.app.description:}") String description) {
		this.name = name;
		this.description = description;
	}

	@Override
	public void contribute(Info.Builder builder) {
		Map<String, Object> app = new LinkedHashMap<>();
		app.put("name", name);
		app.put("description", description);
		builder.withDetail("app", app);
	}
}
