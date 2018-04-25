package xyz.shortbox.backend.rest.service;

import xyz.shortbox.backend.error.Errors;
import xyz.shortbox.backend.exception.HTTPStatusException;
import xyz.shortbox.backend.exception.InternalServerErrorException;
import xyz.shortbox.backend.exception.UnauthorizedException;
import xyz.shortbox.backend.rest.service.filter.AuthenticationFilter;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * Base class for all JAX-RS classes.
 * <p>
 * Contains exception handling for JAX-RS classes.
 * </p>
 */
public class BaseService {
    /**
     * Handles an {@code Exception}.
     * <p>
     * In case the given {@link Exception} is null or an instance of {@link UnauthorizedException}
     * {@code Response.Status.UNAUTHORIZED} is returned.
     * If the given {@code Exception} is null or an instance of {@link HTTPStatusException} and not of
     * {@code UnauthorizedException}, the {@link Response.Status} of the exception is returned.
     * In all other cases {@code Response.Status.INTERNAL_SERVER_ERROR} is returned.
     * </p>
     *
     * @param e the {@code Exception} that should be handled.
     * @return the {@code Response}, that should be returned to the client.
     */
    protected Response handleException(Exception e) {
        if (e == null || e instanceof UnauthorizedException)
            return handleUnauthorizedException((UnauthorizedException) e);
        else if (e instanceof HTTPStatusException)
            return handleHttpStatusException((HTTPStatusException) e);
        else
            return handleInternalErrorException(e);
    }

    private Response handleUnauthorizedException(UnauthorizedException e) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, AuthenticationFilter.AUTHENTICATION_SCHEME + " realm=\"" + AuthenticationFilter.REALM + "\"")
                .entity(e == null ? null : e.getError()).build();
    }

    private Response handleHttpStatusException(HTTPStatusException e) {
        return Response.status(e.getHttpResponseCode()).entity(e.getError()).build();
    }

    private Response handleInternalErrorException(Exception e) {
        InternalServerErrorException internalException = new InternalServerErrorException(Errors.UNKNOWN_ERROR);
        e.printStackTrace();
        return handleHttpStatusException(internalException);
    }
}
