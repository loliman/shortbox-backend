package xyz.shortbox.backend.exception;

import xyz.shortbox.backend.error.Error;

import javax.ws.rs.core.Response;

public class InternalServerErrorException extends HTTPStatusException {
    public InternalServerErrorException(Error error) {
        super(error);
        this.httpResponseCode = Response.Status.INTERNAL_SERVER_ERROR;
    }
}
