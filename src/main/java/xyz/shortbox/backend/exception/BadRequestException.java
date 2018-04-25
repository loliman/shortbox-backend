package xyz.shortbox.backend.exception;

import xyz.shortbox.backend.error.Error;

import javax.ws.rs.core.Response;

public class BadRequestException extends HTTPStatusException {
    public BadRequestException(Error error) {
        super(error);
        this.httpResponseCode = Response.Status.BAD_REQUEST;
    }
}
