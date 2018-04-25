package xyz.shortbox.backend.exception;

import xyz.shortbox.backend.error.Error;

import javax.ws.rs.core.Response;

public class ConflictException extends HTTPStatusException {
    public ConflictException(Error error) {
        super(error);
        this.httpResponseCode = Response.Status.CONFLICT;
    }
}
