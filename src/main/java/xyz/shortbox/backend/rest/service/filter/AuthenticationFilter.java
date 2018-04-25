package xyz.shortbox.backend.rest.service.filter;

import xyz.shortbox.backend.ejb.AuthBean;
import xyz.shortbox.backend.ejb.entity.UserEntity;
import xyz.shortbox.backend.rest.annotation.Secured;
import xyz.shortbox.backend.rest.service.BaseService;
import xyz.shortbox.backend.rest.util.AuthUserProvider;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Searches for a valid session.
 * <p>
 * This {@code Filter} hooks into the authentication step of the request and searches for an valid {@link xyz.shortbox.backend.ejb.entity.SessionEntity}
 * corresponding to the given session token.
 * The session token must be contained in the AUTH Header. Shortbox uses Bearer authentication.
 * If no valid Session for the token is found, the Request will be declined.
 * </p>
 * <p>
 * Idea taken from https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey
 * </p>
 */
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter extends BaseService implements ContainerRequestFilter {

    public static final String REALM = "shortbox.xyz";
    public static final String AUTHENTICATION_SCHEME = "Bearer";

    @Inject
    private AuthUserProvider provider;

    /**
     * Searches for a valid {@code Session} and declines if none exists.
     * <p>
     * If there is no valid {@link xyz.shortbox.backend.ejb.entity.SessionEntity} found for the given session token, the
     * request will be declined with {@link Response.Status}.UNAUTHORIZED.
     * If there is a valid {@code SessionEntity} found for the given session token, the corresponding user and the session
     * token itself will made available to other beans through the {@link AuthUserProvider} and the active {@code SessionEntity}
     * will be updated with the current timestamp.
     * </p>
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abort(requestContext, null);
            return;
        }

        try {
            validateToken(authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim());
        } catch (Exception e) {
            abort(requestContext, e);
        }
    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        return authorizationHeader != null
                && authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }

    private void abort(ContainerRequestContext requestContext, Exception e) {
        requestContext.abortWith(handleException(e));
    }

    private void validateToken(String token) throws Exception {
        AuthBean authBean = (AuthBean) new InitialContext().lookup(AuthBean.JNDI_NAME);
        UserEntity user = authBean.validate(token);

        provider.setAuthenticationToken(token);
        provider.setAuthenticatedUser(user);
    }
}