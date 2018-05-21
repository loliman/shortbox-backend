package xyz.shortbox.backend.ejb;

import xyz.shortbox.backend.dto.ListDTO;
import xyz.shortbox.backend.ejb.entity.*;
import xyz.shortbox.backend.error.Errors;
import xyz.shortbox.backend.exception.ForbiddenException;
import xyz.shortbox.backend.exception.NotFoundException;

import javax.ejb.Stateless;
import java.util.List;

@Stateless
public class SeriesBean extends BaseBean {

    public static String JNDI_NAME = "java:global/api/SeriesBean!xyz.shortbox.backend.ejb.SeriesBean";

    public void deleteList(int id) throws NotFoundException {
        SeriesEntity series = em.find(SeriesEntity.class, id);

        if (series == null)
            throw new NotFoundException(Errors.SERIES_NOT_FOUND);

        List<IssueEntity> issues = em.createNamedQuery("getIssuesBySeries", IssueEntity.class)
                .setParameter("fkSeries", series.getId())
                .getResultList();

        for (IssueEntity issue : issues) {
            List<IssueListEntity> relations = em.createNamedQuery("getILByIssue", IssueListEntity.class)
                    .setParameter("fkIssue", issue.getId())
                    .getResultList();

            for (IssueListEntity il : relations)
                em.remove(il);

            em.remove(issue);
        }

        em.remove(series);
    }
}
