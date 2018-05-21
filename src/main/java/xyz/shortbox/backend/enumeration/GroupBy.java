package xyz.shortbox.backend.enumeration;

/**
 * Defines all different types of group possibilities.
 * <p>
 * This enumeration will be used by {@link xyz.shortbox.backend.ejb.entity.ListEntity} to determine the groupBy
 * column.
 * </p>
 */
public enum GroupBy {
    /**
     * Series (title, year)
     */
    SERIES,

    /**
     * Publisher (name)
     */
    PUBLISHER,

    /**
     * Issue title
     */
    TITLE,

    /**
     * Issue releasedate
     */
    RELEASEDATE
}
