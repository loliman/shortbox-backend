package xyz.shortbox.backend.rest.service.filter;

import xyz.shortbox.backend.enumeration.UserGroup;
import xyz.shortbox.backend.error.Errors;
import xyz.shortbox.backend.exception.ForbiddenException;
import xyz.shortbox.backend.rest.annotation.Secured;
import xyz.shortbox.backend.rest.service.BaseService;
import xyz.shortbox.backend.rest.util.AuthUserProvider;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Checks weather the currently logged in user is allowed to use the restful method.
 * <p>
 * This {@code Filter} hooks into the authorization step of the request and checks weather the currently logged in
 * {@link xyz.shortbox.backend.ejb.entity.UserEntity} is has access to the called restful method.
 * To do that the {@code UserEntity} is pulled out of the {@link AuthUserProvider} and is checked against the
 * {@link Secured} annotated {@link UserGroup}.
 * If the user is not allowed to call the method, the Request will be declined.
 * </p>
 * <p>
 * Idea taken from https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey
 * </p>
 */
@Secured
@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter extends BaseService implements ContainerRequestFilter {

    @Inject
    private AuthUserProvider producer;

    @Context
    private ResourceInfo resourceInfo;

    /**
     * Checks weather the currently logged in user is allowed to use the restful method.
     * <p>
     * If the {@link xyz.shortbox.backend.ejb.entity.UserEntity} is not in one the {@link Secured} annotated {@link UserGroup}
     * request will be declined with {@link Response.Status}.NOT_ALLOWED.
     * Otherwise the {@code UserEntity} will be granted access.
     * </p>
     *
     * @param requestContext the current {@link com.sun.xml.internal.ws.client.RequestContext}.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        List<UserGroup> classRoles = extractRoles(resourceInfo.getResourceClass());
        List<UserGroup> methodRoles = extractRoles(resourceInfo.getResourceMethod());

        try {
            if (!classRoles.isEmpty())
                checkPermissions(classRoles);
            else if (!methodRoles.isEmpty())
                checkPermissions(methodRoles);
        } catch (Exception e) {
            requestContext.abortWith(handleException(e));
        }
    }

    private List<UserGroup> extractRoles(AnnotatedElement annotatedElement) {
        if (annotatedElement == null)
            return new ArrayList<>();

        Secured secured = annotatedElement.getAnnotation(Secured.class);
        return secured == null ? new ArrayList<>() : Arrays.asList(secured.value());
    }

    private void checkPermissions(List<UserGroup> allowedRoles) throws Exception {
        boolean allowed = false;

        for (UserGroup role : allowedRoles)
            if (producer.getAuthenticatedUser().getUsergroup().equals(role.name())) {
                allowed = true;
                break;
            }

        if (!allowed)
            throw new ForbiddenException(Errors.NOT_ALLOWED);
    }
}