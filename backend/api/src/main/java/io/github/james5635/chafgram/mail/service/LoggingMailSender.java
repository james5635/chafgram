package io.github.james5635.chafgram.mail.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Development fallback: writes the message to the application log instead of
 * sending it. Keeps the account flows exercisable end-to-end on a fresh clone
 * with no SMTP server (and honours BR-10.1 — no outbound calls).
 * <p>
 * Bearer tokens are credential material, so the logged copy redacts them
 * (AGENTS.md: never log credential material). Only the raw token is removed —
 * the rest of the message stays readable for local development.
 */
public class LoggingMailSender implements ChafgramMailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

	/** {@code ?token=…} — the account reset/verification links. */
	private static final Pattern TOKEN = Pattern.compile("(token=)[^\\s&]+", Pattern.CASE_INSENSITIVE);

	@Override
	public void send(String to, String subject, String text) {
		log.info("Chafgram mail (not sent — no SMTP configured)\nTo: {}\nSubject: {}\n{}",
				to, subject, redact(text));
	}

	/** Replaces every token value with a placeholder; leaves other text alone. */
	static String redact(String text) {
		return text == null ? "" : TOKEN.matcher(text).replaceAll("$1<redacted>");
	}
}
