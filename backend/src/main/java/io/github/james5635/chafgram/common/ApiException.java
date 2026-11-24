package io.github.james5635.chafgram.common;

import org.springframework.http.HttpStatusCode;

/**
 * An error that is safe to surface to API clients. Carries an HTTP status
 * and a stable, human-readable detail message.
 */
public class ApiException extends RuntimeException {

	private final HttpStatusCode status;

	public ApiException(HttpStatusCode status, String detail) {
		super(detail);
		this.status = status;
	}

	public HttpStatusCode getStatus() {
		return status;
	}
}
