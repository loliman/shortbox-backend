package xyz.shortbox.backend.rest.util;

import xyz.shortbox.backend.ejb.entity.UserEntity;

import javax.enterprise.context.RequestScoped;

/**
 * Provides the currently logged in {@code UserEntity}.
 * <p>
 * Serves as a bridge between {@link xyz.shortbox.backend.rest.service.filter.AuthenticationFilter} and every injecting
 * service.
 * Provides the currently {@link UserEntity} for this session and the corresponding session token.
 * </p>
 */
@RequestScoped
public class AuthUserProvider {

    private UserEntity authenticatedUser;
    private String authenticationToken;

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    public UserEntity getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(UserEntity authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }
}