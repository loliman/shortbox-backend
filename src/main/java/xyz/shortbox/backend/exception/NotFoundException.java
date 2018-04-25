package xyz.shortbox.backend.exception;

import xyz.shortbox.backend.error.Error;

import javax.ws.rs.core.Response;

public class NotFoundException extends HTTPStatusException {
    public NotFoundException(Error error) {
        super(error);
        this.httpResponseCode = Response.Status.NOT_FOUND;
    }
}
