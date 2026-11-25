package io.github.james5635.chafgram.seed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Headless Spring Boot entry point for dev fixtures.
 *
 * <p>Reuses the application services from {@code :api} but deliberately does
 * not scan controllers or the web security configuration: seeding goes
 * straight through the service layer, with the acting member passed in.</p>
 */
@EnableAutoConfiguration
@ComponentScan(
    basePackages = "io.github.james5635.chafgram",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.controller\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.ChafgramApplication"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.config\\.SecurityConfiguration"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.common\\.utils\\.SessionAuthenticator"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.identity\\.service\\.PasskeyService")
    }
)
@EntityScan(basePackages = "io.github.james5635.chafgram")
@EnableJpaRepositories(basePackages = "io.github.james5635.chafgram")
public class SeedApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(SeedApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }

    /** The application's security config is excluded, so provide the bean services need. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
