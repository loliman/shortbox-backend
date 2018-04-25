package xyz.shortbox.backend.rest.annotation;

import xyz.shortbox.backend.enumeration.UserGroup;

import javax.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the security Level of Restful methods.
 * <p>
 * A secured method can only be accessed if the session request contains a valid session token.
 * </p>
 * <p>
 * This annotation can be applied on class or method level.
 * If it is annotated on class level, every Restful method defined in this class will be secured.
 * If it is annotated on method level, only the annotated method will be secured.
 * </p>
 * <p>
 * As an addition to the session token the methods can be made accessible only for specific {@link UserGroup}.
 * To do so the {@code UserGroup} must be provided in the annotation.
 * Multiple {@code UserGroup} are possible.
 * </p>
 * <p>
 * Idea taken from https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey
 * </p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@NameBinding
public @interface Secured {
    /**
     * The {@code UserGroup} that should have access to secured method.
     * <p>
     * If empty all {@link UserGroup} will have access to the method.
     * </p>
     *
     * @return all assigned {@code UserGroup}
     */
    UserGroup[] value() default {};
}
