package xyz.shortbox.backend.enumeration;

/**
 * Defines all possible sort directions.
 * <p>
 * Will be used by {@link xyz.shortbox.backend.ejb.entity.ListEntity} to determine the sort direction of
 * {@link GroupBy}.
 * </p>
 */
public enum SortDirection {
    /**
     * Ascending
     */
    ASC,

    /**
     * Descending
     */
    DESC
}
