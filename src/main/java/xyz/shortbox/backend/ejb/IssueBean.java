package xyz.shortbox.backend.ejb;

import xyz.shortbox.backend.dto.*;
import xyz.shortbox.backend.ejb.entity.*;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.List;

/**
 * Contains all Methods that are used to retrieve and manipulate Issue data.
 * <p>
 * The data of all returned and modified objects will be retrieved directly from the underlying database.
 * All data, that is returned from any method of this class, will be filled recursively for all child DTOs.
 * Every manipulated object will be persisted in the underlying database directly.
 * </p>
 */
@Stateless
public class IssueBean extends BaseBean {

    public static String JNDI_NAME = "java:global/api/IssueBean!xyz.shortbox.backend.ejb.IssueBean";

    /**
     * Returns an {@code IssueDTO} corresponding to the ID.
     * <p>
     * It doesn't matter whether the {@link IssueDTO} is original or not.
     * Original Issues will not contain Variants and Lists.
     * Also all included {@link StoryDTO}s will be stripped down to its original {@code IssueDTO}
     * and {@link SeriesDTO} (without IDs).
     * </p>
     *
     * @param id the ID of the {@code IssueDTO}.
     * @return the {@code IssueDTO} corresponding to the ID.
     * @throws NoResultException if no Issue for ID is found.
     */
    public IssueDTO getIssue(int id) throws NoResultException {
        IssueDTO issue = findIssue(id);

        if (issue == null)
            return null;

        fillIssueWithStories(issue);

        if (!issue.getSeries().isOriginal()) {
            checkIfIssueIsVariant(issue);
            fillIssueWithVariants(issue);
            fillIssueWithLists(issue);
        }

        clearUnnecessaryFields(issue);

        return issue;
    }

    /**
     * Clears all unnecessary fields from the {@code IssueDTO}.
     * <p>
     * Additionally all related DTOs such as {@link SeriesDTO}, {@link PublisherDTO} and
     * {@link StoryDTO}s will be cleared.
     * The empty fields will not be serialized afterwards.
     * </p>
     *
     * @param issue the {@code IssueDTO} that should be cleared.
     */
    private void clearUnnecessaryFields(IssueDTO issue) {
        clearIssue(issue);

        issue.getStories().forEach(s -> {
            s.setId(null);
            s.setTitle(null);

            if (issue.getSeries().isOriginal()) {
                s.getIssues().forEach(i -> {
                    clearIssue(i);

                    i.getSeries().setId(null);
                    i.getSeries().getPublisher().setId(null);
                });
            } else {
                clearIssue(s.getOriginalIssue());

                s.getOriginalIssue().getSeries().getPublisher().setCreated(null);
                s.getOriginalIssue().getSeries().getPublisher().setLastupdate(null);
            }
        });
    }

    /**
     * Clears all unnecessary fields from the {@code IssueDTO}.
     *
     * @param issue the {@link IssueDTO} that should be cleared.
     */
    private void clearIssue(IssueDTO issue) {
        issue.getSeries().setEndyear(null);
        issue.getSeries().setCreated(null);
        issue.getSeries().setLastupdate(null);

        issue.getSeries().getPublisher().setCreated(null);
        issue.getSeries().getPublisher().setLastupdate(null);
    }

    /**
     * Fills the {@code IssueDTO} with all related variants.
     * <p>
     * The variants will only contain the fields ID, Format and Variant.
     * All Variants are of type {@link IssueDTO}.
     * </p>
     *
     * @param issue the {@code IssueDTO} that should be filled.
     */
    private void fillIssueWithVariants(IssueDTO issue) {
        List<IssueEntity> variantEntities = em.createNamedQuery("findVariantsForIssue", IssueEntity.class)
                .setParameter("fkIssue", issue.getVariantOf() != null ? issue.getVariantOf() : issue.getId())
                .setParameter("fkVariant", issue.getId())
                .getResultList();

        if (variantEntities != null)
            variantEntities.forEach(variant -> {
                IssueDTO v = new IssueDTO();
                v.setId(variant.getId());
                v.setFormat(variant.getFormat());
                v.setVariant(variant.getVariant());

                issue.getVariants().add(v);
            });
    }

    /**
     * Fills the {@code IssueDTO} with all related {@code ListDTO}s.
     * <p>
     * The {@link ListDTO} will only contain the fields ID, Name and Sort.
     * Only {@code ListDTO}, that are created by the current user will be included.
     * </p>
     *
     * @param issue the {@link IssueDTO} object that should be filled.
     */
    private void fillIssueWithLists(IssueDTO issue) {
        List<ListEntity> listEntities = em.createNamedQuery("findListsForIssue", ListEntity.class)
                .setParameter("fkIssue", issue.getId())
                .getResultList();

        if (listEntities != null)
            listEntities.forEach(list -> {
                ListDTO l = new ListDTO();
                l.setId(list.getId());
                l.setName(list.getName());
                l.setSort(list.getSort());

                issue.getLists().add(l);
            });
    }

    /**
     * Fills the {@code IssueDTO} with all related {@code StoryDTO}s.
     * <p>
     * All related child DTOs, will be filled recursively.
     * </p>
     *
     * @param issue the {@link IssueDTO} that should be filled.
     */
    private void fillIssueWithStories(IssueDTO issue) {
        Collection<StoryEntity> storyEntities = em.createNamedQuery("findAllStoriesForIssue", StoryEntity.class)
                .setParameter("fkIssue", issue.getId())
                .getResultList();

        if (storyEntities != null)
            storyEntities.forEach(story -> {
                StoryDTO s = (StoryDTO) story.toDTO();
                fillStoryWithIssues(s, issue);
                issue.getStories().add(s);
            });
    }

    /**
     * Fills the {@code StoryDTO} with all related {@code IssueDTO}s.
     * <p>
     * This {@link IssueDTO} and all Variants won't be included.
     * </p>
     * <p>
     * The {@code IssueDTO}s will only be filled with ID, Number and Series.
     * All related child DTOs will be filled recursively.
     * For non-original {@code IssueDTO}s the issuelist field will be empty.
     * Issuecount will be set instead.
     * </p>
     *
     * @param story     the {@code StoryDTO} that should be filled
     * @param mainIssue the main {@code IssueDTO} that should not be in the issuelist of given {@code StoryDTO}
     */
    private void fillStoryWithIssues(StoryDTO story, IssueDTO mainIssue) {
        List<IssueEntity> issueEntities = em.createNamedQuery("getIssuesForStoryWithoutMain", IssueEntity.class)
                .setParameter("fkStory", story.getId())
                .setParameter("fkIssue", mainIssue.getId())
                .getResultList();

        if (issueEntities != null)
            issueEntities.forEach(ie -> {
                SeriesDTO series = findSeries(ie.getFkSeries());

                IssueDTO i = new IssueDTO();
                i.setId(ie.getId());
                i.setNumber(ie.getNumber());
                i.setSeries(series);

                if (mainIssue.getSeries().isOriginal() && !i.getSeries().isOriginal()) {
                    i.setSeries(series);
                    story.getIssues().add(i);
                } else if (!mainIssue.getSeries().isOriginal() && i.getSeries().isOriginal()) {
                    story.setOriginalIssue(i);
                    story.setIssueCount(issueEntities.size());
                }
            });
    }

    /**
     * Checks if the {@code IssueDTO} is a Variant.
     * <p>
     * If the {@link IssueDTO} is a variant the main {@code IssueDTO} will be set.
     * The main {@code IssueDTO} will only be filled with ID and Format.
     * </p>
     *
     * @param issue the {@code IssueDTO} that should be filled
     */
    private void checkIfIssueIsVariant(IssueDTO issue) {
        try {
            Object[] mainIssueResult = (Object[]) em.createNamedQuery("findMainIssueForVariant")
                    .setParameter("fkVariant", issue.getId())
                    .getSingleResult();

            IssueDTO mainIssue = new IssueDTO();
            mainIssue.setId((Integer) mainIssueResult[0]);
            mainIssue.setFormat((String) mainIssueResult[1]);

            issue.setVariantOf(mainIssue);
        } catch (NoResultException nre) {
            //It's okay, it's the main issue
        }
    }

    /**
     * Returns an {@code IssueDTO} corresponding to the given ID.
     * <p>
     * Also the {@link StoryDTO} and {@link PublisherDTO} will be set.
     * </p>
     *
     * @param id the ID of the {@link IssueDTO} that should be returned, null if no Issue is found.
     * @return the {@code IssueDTO} corresponding to the ID.
     * @throws NoResultException if no {@code IssueDTO} for ID is found.
     */
    private IssueDTO findIssue(int id) throws NoResultException {
        IssueEntity entity = em.find(IssueEntity.class, id);

        if (entity == null)
            throw new NoResultException();

        IssueDTO issue = (IssueDTO) entity.toDTO();
        SeriesDTO series = findSeries(issue.getSeries().getId());
        issue.setSeries(series);

        return issue;
    }

    /**
     * Returns an {@code SeriesDTO} corresponding to the given ID.
     * <p>
     * Also the {@link PublisherDTO} will be set.
     * </p>
     *
     * @param id the ID of the {@link SeriesDTO} that should be returned.
     * @return the {@code SeriesDTO} corresponding to the ID.
     */
    private SeriesDTO findSeries(int id) {
        SeriesDTO series = (SeriesDTO) em.find(SeriesEntity.class, id).toDTO();
        PublisherDTO publisher = findPublisher(series.getPublisher().getId());
        series.setPublisher(publisher);

        return series;
    }

    /**
     * Returns an {@code PublisherDTO} corresponding to the given ID.
     *
     * @param id the ID of the {@link PublisherDTO} that should be returned.
     * @return the {@code PublisherDTO} corresponding to the ID.
     */
    private PublisherDTO findPublisher(int id) {
        return (PublisherDTO) em.find(PublisherEntity.class, id).toDTO();
    }
}
