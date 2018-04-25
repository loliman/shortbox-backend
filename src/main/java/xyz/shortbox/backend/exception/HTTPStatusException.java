package xyz.shortbox.backend.exception;

import xyz.shortbox.backend.error.Error;

import javax.ws.rs.core.Response;

/**
 * Base class for internal Shortbox Exceptions.
 * <p>
 * Shortbox internal {@code exceptions} always contain a {@link Response.Status} that will be send to the client and an
 * extended error message.
 * </p>
 * <p>
 * All extending {@code exceptions} MUST set the correct {@code Response.Status} in its constructor and SHOULD follow
 * the naming convention "ResponsecodeName"Exception.
 * The error message SHOULD be an {@link Error} defined in {@link xyz.shortbox.backend.error.Errors}.
 * </p>
 */
public abstract class HTTPStatusException extends Exception {
    Response.Status httpResponseCode;
    private Error error;

    public HTTPStatusException() {

    }

    HTTPStatusException(Error error) {
        this.error = error;
    }

    public Response.Status getHttpResponseCode() {
        return httpResponseCode;
    }

    public Error getError() {
        return error;
    }
}
