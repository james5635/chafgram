package io.github.james5635.chafgram.mail;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dev fallback writes account mail to the application log; bearer tokens
 * are credential material and must never reach it (AGENTS.md: never log
 * credential material).
 */
class LoggingMailSenderTest {

	@Test
	void redactsBearerTokensFromTheLoggedBody() {
		Logger logger = (Logger) LoggerFactory.getLogger(LoggingMailSender.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		String logged;
		try {
			new LoggingMailSender().send("alice@example.com", "Reset your Chafgram password",
					"Reset link: http://localhost:3000/reset-password?token=SECRET-TOKEN-123");
			logged = appender.list.stream()
					.map(ILoggingEvent::getFormattedMessage)
					.collect(Collectors.joining("\n"));
		}
		finally {
			logger.detachAppender(appender);
		}

		assertThat(logged)
				.as("the raw token must never reach the log")
				.doesNotContain("SECRET-TOKEN-123");
		// the rest of the message stays useful for local development
		assertThat(logged).contains("reset-password");
	}
}
