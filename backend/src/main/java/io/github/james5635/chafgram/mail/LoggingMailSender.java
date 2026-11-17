package io.github.james5635.chafgram.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development fallback: writes the message to the application log instead of
 * sending it. Keeps the account flows exercisable end-to-end on a fresh clone
 * with no SMTP server (and honours BR-10.1 — no outbound calls).
 */
public class LoggingMailSender implements ChafgramMailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

	@Override
	public void send(String to, String subject, String text) {
		log.info("Chafgram mail (not sent — no SMTP configured)\nTo: {}\nSubject: {}\n{}", to, subject, text);
	}
}
