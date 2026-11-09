package com.islamshariful.spring_mcp_client.web;

import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import io.modelcontextprotocol.spec.McpError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns errors into short, clear JSON messages instead of a generic "500 Internal Server Error".
 *
 * <pre>{"status": 503, "error": "Claude API key is missing or invalid. Set ANTHROPIC_API_KEY."}</pre>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record ErrorResponse(int status, String error) {}

    /** Bad input from the caller, e.g. an empty message. */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ErrorResponse> unreadableRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request: " + e.getMessage());
    }

    /** The MCP server answered with an error, e.g. "Authentication required" or an unknown prompt. */
    @ExceptionHandler(McpError.class)
    ResponseEntity<ErrorResponse> mcpError(McpError e) {
        log.warn("MCP server returned an error: {}", e.getMessage());
        return error(HttpStatus.BAD_GATEWAY, "MCP server error: " + e.getMessage());
    }

    /** No MCP server connected (see McpExplorerService). */
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorResponse> notReady(IllegalStateException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ErrorResponse> claudeUnauthorized(UnauthorizedException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Claude API key is missing or invalid. Set ANTHROPIC_API_KEY.");
    }

    @ExceptionHandler(RateLimitException.class)
    ResponseEntity<ErrorResponse> claudeRateLimited(RateLimitException e) {
        return error(HttpStatus.TOO_MANY_REQUESTS, "Claude rate limit reached. Wait a moment and try again.");
    }

    @ExceptionHandler(AnthropicServiceException.class)
    ResponseEntity<ErrorResponse> claudeError(AnthropicServiceException e) {
        log.warn("Claude API error ({}): {}", e.statusCode(), e.getMessage());
        return error(HttpStatus.BAD_GATEWAY, "Claude API error (" + e.statusCode() + "). Check the logs for details.");
    }

    /** Anything else: log the details, return a short message. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        log.error("Unexpected error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Check the logs for details.");
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message));
    }
}
