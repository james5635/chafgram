package io.github.james5635.chafgram.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Sends account mail over SMTP. Only active when the admin explicitly turns
 * it on (see {@code MailConfiguration}).
 */
public class SmtpMailSender implements ChafgramMailSender {

	private final JavaMailSender sender;
	private final String from;

	public SmtpMailSender(JavaMailSender sender, String from) {
		this.sender = sender;
		this.from = from;
	}

	@Override
	public void send(String to, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		sender.send(message);
	}
}
