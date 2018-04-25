package xyz.shortbox.backend.dto;

import xyz.shortbox.backend.ejb.entity.BaseEntity;

/**
 * Base object of all DTO classes.
 * <p>
 * This class should be implemented by all DTO classes.
 * It defines some useful abstract methods, that must be overridden by the implementing class.
 * </p>
 */
public abstract class BaseDTO {

    /**
     * Compares to {@code BaseDTO} objects.
     * <p>
     * Both DTOs, this and the one that should be compared with, must be from the same type.
     * Each and every member of the {@link BaseDTO} will be compared.
     * </p>
     *
     * @param dto the {@code Object} that this {@code BaseDTO} should be compared with, null returns false.
     * @return true if DTOs are the same, false if not.
     */
    public abstract boolean equals(BaseDTO dto);

    /**
     * Returns a hash code value for the {@code BaseDTO}.
     * <p>
     * This method is supported for the benefit of hash tables such as those provided by HashMap.
     * </p>
     *
     * @return a hash code value for this object.
     */
    public abstract int hashCode();

    /**
     * Converts this {@code BaseDTO} into an {@code BaseEntity}.
     * <p>
     * The {@link BaseEntity} will be an entity representation of this {@link BaseDTO}, filled with all available
     * members.
     * Child DTOs will be represented as Integers containing their ID.
     *
     * @return the converted {@code BaseDTO} as {@code BaseEntity}.
     */
    public abstract BaseEntity toEntity();
}
