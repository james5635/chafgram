package io.github.james5635.chafgram.common;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Maps {@link ApiException} — the service-layer "safe to surface" error — onto
 * the GraphQL error contract.
 *
 * <p>GraphQL answers every failure with HTTP 200 plus an {@code errors[]}
 * array; the machine-readable category lives in
 * {@code extensions.classification}. Without this handler Spring GraphQL
 * treats an unhandled exception as {@code INTERNAL_ERROR} and replaces the
 * message with an opaque {@code INTERNAL_ERROR for <executionId>} string, so
 * clients cannot tell "forbidden" from "server bug" and the useful detail is
 * lost.</p>
 *
 * <p>Spring Security's own failures ({@code @PreAuthorize} denials) are
 * resolved upstream by Spring GraphQL and keep their own classification.</p>
 */
@ControllerAdvice
public class GraphQlApiExceptionHandler {

	@GraphQlExceptionHandler
	public GraphQLError handleApiException(ApiException ex) {
		return GraphqlErrorBuilder.newError()
				.errorType(classify(ex.getStatus()))
				.message(ex.getMessage())
				.build();
	}

	private static ErrorType classify(HttpStatusCode status) {
		if (status instanceof HttpStatus http) {
			return switch (http) {
				case BAD_REQUEST -> ErrorType.BAD_REQUEST;
				case UNAUTHORIZED -> ErrorType.UNAUTHORIZED;
				case FORBIDDEN -> ErrorType.FORBIDDEN;
				case NOT_FOUND -> ErrorType.NOT_FOUND;
				default -> ErrorType.INTERNAL_ERROR;
			};
		}
		return switch (status.value()) {
			case 400 -> ErrorType.BAD_REQUEST;
			case 401 -> ErrorType.UNAUTHORIZED;
			case 403 -> ErrorType.FORBIDDEN;
			case 404 -> ErrorType.NOT_FOUND;
			default -> ErrorType.INTERNAL_ERROR;
		};
	}
}
