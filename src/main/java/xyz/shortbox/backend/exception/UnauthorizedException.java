package xyz.shortbox.backend.exception;

import xyz.shortbox.backend.error.Error;

import javax.ws.rs.core.Response;

public class UnauthorizedException extends HTTPStatusException {
    public UnauthorizedException(Error error) {
        super(error);
        this.httpResponseCode = Response.Status.UNAUTHORIZED;
    }
}
