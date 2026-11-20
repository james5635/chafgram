package io.github.james5635.chafgram.config;

import io.github.james5635.chafgram.mail.ChafgramMailSender;
import io.github.james5635.chafgram.mail.LoggingMailSender;
import io.github.james5635.chafgram.mail.SmtpMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Picks the mail transport (FR-1.7).
 * <p>
 * SMTP is opt-in: unless {@code CHAFGRAM_MAIL_SMTP_ENABLED=true} is set,
 * account mail is written to the application log instead of leaving the
 * instance — which also keeps air-gapped installs working (BR-10.1).
 */
@Configuration
public class MailConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "chafgram.mail", name = "smtp-enabled", havingValue = "true")
	ChafgramMailSender smtpMailSender(ObjectProvider<JavaMailSender> javaMailSender,
			@Value("${chafgram.mail.from:chafgram@localhost}") String from) {
		JavaMailSender sender = javaMailSender.getIfAvailable();
		if (sender == null) {
			throw new IllegalStateException(
					"CHAFGRAM_MAIL_SMTP_ENABLED=true but no SMTP host is configured — set SPRING_MAIL_HOST");
		}
		return new SmtpMailSender(sender, from);
	}

	@Bean
	@ConditionalOnProperty(prefix = "chafgram.mail", name = "smtp-enabled",
			havingValue = "false", matchIfMissing = true)
	ChafgramMailSender loggingMailSender() {
		return new LoggingMailSender();
	}
}
