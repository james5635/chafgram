package io.github.james5635.chafgram.common;

import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

/**
 * Maps exceptions to RFC 7807 problem responses. Never leaks stack traces.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	ProblemDetail handleApiException(ApiException ex) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
		return detail;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed: " + detail);
		problem.setTitle("Invalid request");
		return problem;
	}
}
