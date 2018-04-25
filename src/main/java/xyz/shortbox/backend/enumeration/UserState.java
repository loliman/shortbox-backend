package xyz.shortbox.backend.enumeration;

/**
 * Defines all different UserState.
 * <p>
 * This UserState will be reflected in the {@link xyz.shortbox.backend.ejb.entity.UserEntity} and represented in the
 * database.
 * They will be represented as their {@code name()} values.
 * </p>
 */
public enum UserState {
    /**
     * Occures if a User started the regestration process and did not activate the account using the link in the
     * regestration mail.
     */
    REGISTER,

    /**
     * Occures if a User started the forgot password process and did not reset the password using the link in the
     * regestration mail.
     */
    FORGOT_PW,

    /**
     * Occures if a User finished the registration successfully and dit not start the forgot password process.
     */
    ACTIVE
}
