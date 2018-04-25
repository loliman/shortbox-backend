package xyz.shortbox.backend.exception;

import xyz.shortbox.backend.error.Error;

import javax.ws.rs.core.Response;

public class ForbiddenException extends HTTPStatusException {
    public ForbiddenException(Error error) {
        super(error);
        this.httpResponseCode = Response.Status.FORBIDDEN;
    }
}
