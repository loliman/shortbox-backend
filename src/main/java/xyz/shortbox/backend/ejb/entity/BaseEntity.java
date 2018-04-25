package xyz.shortbox.backend.ejb.entity;

import xyz.shortbox.backend.dto.BaseDTO;

/**
 * Base object of all entity classes.
 * <p>
 * This class should be implemented by all entity classes.
 * It defines some useful abstract methods, that must be overridden by the implementing class.
 * </p>
 */
public abstract class BaseEntity {

    /**
     * Compares to {@code BaseEntity} objects.
     * <p>
     * Both entities, this and the one that should be compared with, must be from the same type.
     * Each and every member of the {@link BaseEntity} will be compared.
     * </p>
     *
     * @param entity the {@code Object} that this {@code BaseEntity} should be compared with, null returns false.
     * @return true if entities are the same, false if not.
     */
    public abstract boolean equals(BaseEntity entity);

    /**
     * Returns a hash code value for the {@code BaseEntity}.
     * <p>
     * This method is supported for the benefit of hash tables such as those provided by HashMap.
     * </p>
     *
     * @return a hash code value for this object.
     */
    public abstract int hashCode();

    /**
     * Converts this {@code BaseEntity} into an {@code BaseDTO}.
     * <p>
     * The {@link BaseDTO} will be an entity representation of this {@link BaseEntity}, filled with all available
     * members.
     * Child entities will be represented as empty DTOs containing only their ID.
     *
     * @return the converted {@code BaseEntity} as {@code BaseDTO}.
     */
    public abstract BaseDTO toDTO();
}
