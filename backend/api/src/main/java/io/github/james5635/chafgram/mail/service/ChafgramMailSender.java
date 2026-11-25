package io.github.james5635.chafgram.mail.service;

/**
 * Outbound mail for account flows (FR-1.7).
 * <p>
 * Chafgram never sends notification mail (BR-7.1) — this port exists only for
 * the opt-in email-verification and password-reset flows, both of which the
 * member or an admin explicitly asks for.
 */
public interface ChafgramMailSender {

	/**
	 * Sends a plain-text message.
	 *
	 * @param to      recipient address
	 * @param subject message subject
	 * @param text    message body
	 */
	void send(String to, String subject, String text);
}
