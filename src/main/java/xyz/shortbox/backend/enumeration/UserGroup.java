package xyz.shortbox.backend.enumeration;

/**
 * Defines all different UserGroup.
 * <p>
 * This UserGroup will be reflected in the {@link xyz.shortbox.backend.ejb.entity.UserEntity} and represented in the
 * database.
 * They will be represented as their {@code name()} values.
 * </p>
 */
public enum UserGroup {
    /**
     * Administrative User.
     */
    ADMINISTRATOR,

    /**
     * Normal User.
     * <p>
     * Will be set through the regular registration process.
     * </p>
     */
    USER,

    /**
     * Internal User, which will be used if the user is not logged in.
     */
    GUEST
}
