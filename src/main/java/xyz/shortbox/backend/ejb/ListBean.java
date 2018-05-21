package xyz.shortbox.backend.ejb;

import xyz.shortbox.backend.dto.ListDTO;
import xyz.shortbox.backend.ejb.entity.IssueEntity;
import xyz.shortbox.backend.ejb.entity.IssueListEntity;
import xyz.shortbox.backend.ejb.entity.ListEntity;
import xyz.shortbox.backend.ejb.entity.UserEntity;
import xyz.shortbox.backend.enumeration.GroupBy;
import xyz.shortbox.backend.enumeration.SortDirection;
import xyz.shortbox.backend.error.Errors;
import xyz.shortbox.backend.exception.ConflictException;
import xyz.shortbox.backend.exception.ForbiddenException;
import xyz.shortbox.backend.exception.NotFoundException;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains all Methods that are used to retrieve and manipulate List data.
 * <p>
 * The data of all returned and modified objects will be retrieved directly from the underlying database.
 * Every manipulated object will be persisted in the underlying database directly.
 * </p>
 */
@Stateless
public class ListBean extends BaseBean {

    public static String JNDI_NAME = "java:global/api/ListBean!xyz.shortbox.backend.ejb.ListBean";

    public ListDTO getList(int id, UserEntity user) throws NotFoundException, ForbiddenException {
        ListEntity entity = em.find(ListEntity.class, id);

        if (entity == null)
            throw new NotFoundException(Errors.LIST_NOT_FOUND);

        if (entity.getFkUser() != user.getId())
            throw new ForbiddenException(Errors.NOT_OWN_LIST);

        return (ListDTO) entity.toDTO();
    }

    public void createList(String name, int sort, String groupby, UserEntity user) throws ConflictException {
        if (sort == 0) {
            try {
                sort = em.createNamedQuery("getLastListByUser", Integer.class)
                        .setParameter("fkUser", user.getId())
                        .getSingleResult();

                sort = sort + 1;
            } catch (NoResultException e) {
                sort = 1;
            }
        }

        if (groupby == null || groupby.isEmpty())
            groupby = GroupBy.SERIES.name() + " " + SortDirection.ASC.name();

        ListEntity list = null;

        try {
            list = em.createNamedQuery("getListByNameAndUser", ListEntity.class)
                    .setParameter("name", name)
                    .setParameter("fkUser", user.getId())
                    .getSingleResult();
        } catch (Exception e) {
            //great, there is no user!
        }

        if (list != null)
            throw new ConflictException(Errors.LIST_ALREADY_EXISTS);

        list = new ListEntity();
        list.setName(name);
        list.setFkUser(user.getId());
        list.setSort(sort);
        list.setGroupby(groupby);

        em.persist(list);
    }

    public List<ListDTO> getLists(UserEntity user) {
        List<ListDTO> lists = new ArrayList<>();

        List<ListEntity> entities = em.createNamedQuery("getListsByUser", ListEntity.class)
                .setParameter("fkUser", user.getId())
                .getResultList();

        entities.forEach(e -> {
            ListDTO list = (ListDTO) e.toDTO();
            e.setGroupby(null);
            lists.add(list);
        });

        return lists;
    }

    public void clearList(int id, UserEntity user) throws NotFoundException, ForbiddenException {
        ListEntity list = em.find(ListEntity.class, id);

        if (list == null)
            throw new NotFoundException(Errors.LIST_NOT_FOUND);

        if (list.getFkUser() != user.getId())
            throw new ForbiddenException(Errors.NOT_OWN_LIST);

        List<IssueListEntity> relations = em.createNamedQuery("getILByList", IssueListEntity.class)
                .setParameter("fkList", list.getId())
                .getResultList();

        relations.forEach(relation -> em.remove(relation));
    }

    public void addIssue(int listId, int issueId, boolean increase, UserEntity user) throws NotFoundException, ForbiddenException, ConflictException {
        ListEntity list = em.find(ListEntity.class, listId);

        if (list == null)
            throw new NotFoundException(Errors.LIST_NOT_FOUND);

        if (list.getFkUser() != user.getId())
            throw new ForbiddenException(Errors.NOT_OWN_LIST);

        IssueEntity issue = em.find(IssueEntity.class, issueId);

        if (issue == null)
            throw new NotFoundException(Errors.ISSUE_NOT_FOUND);

        IssueListEntity il = null;

        try {
            il = em.createNamedQuery("getIssueListByIds", IssueListEntity.class)
                    .setParameter("fkList", listId)
                    .setParameter("fkIssue", issueId)
                    .getSingleResult();

            if (increase) {
                il.setAmount(il.getAmount() + 1);
                em.merge(il);
            } else {
                throw new ConflictException(Errors.ISSUE_ALREADY_ON_LIST);
            }
        } catch (Exception e) {
            //great, there is no il!
        }

        il = new IssueListEntity();
        il.setFkIssue(issueId);
        il.setFkList(listId);
        em.persist(il);
    }

    public void removeIssue(int listId, int issueId, boolean decrease, UserEntity user) throws NotFoundException, ForbiddenException, ConflictException {
        ListEntity list = em.find(ListEntity.class, listId);

        if (list == null)
            throw new NotFoundException(Errors.LIST_NOT_FOUND);

        if (list.getFkUser() != user.getId())
            throw new ForbiddenException(Errors.NOT_OWN_LIST);

        IssueEntity issue = em.find(IssueEntity.class, issueId);

        if (issue == null)
            throw new NotFoundException(Errors.ISSUE_NOT_FOUND);

        IssueListEntity il = null;

        try {
            il = em.createNamedQuery("getIssueListByIds", IssueListEntity.class)
                    .setParameter("fkList", listId)
                    .setParameter("fkIssue", issueId)
                    .getSingleResult();

            if (decrease) {
                il.setAmount(il.getAmount() - 1);

                if (il.getAmount() <= 0)
                    em.remove(il);
                else
                    em.merge(il);
            } else {
                em.remove(il);
            }
        } catch (Exception e) {
            //great, there is no il!
        }

        if (il != null)
            throw new ConflictException(Errors.ISSUE_NOT_ON_LIST);
    }

    public void deleteList(int listId, UserEntity user) throws NotFoundException, ForbiddenException {
        clearList(listId, user);

        ListEntity list = em.find(ListEntity.class, listId);

        em.remove(list);
    }

    public void editList(ListDTO dto, UserEntity user) throws NotFoundException, ForbiddenException, ConflictException {
        ListEntity list = em.find(ListEntity.class, dto.getId());

        if (list == null)
            throw new NotFoundException(Errors.LIST_NOT_FOUND);

        if (list.getFkUser() != user.getId())
            throw new ForbiddenException(Errors.NOT_OWN_LIST);

        if (dto.getSort() == 0) {
            try {
                int sort = em.createNamedQuery("getLastListByUser", Integer.class)
                        .setParameter("fkUser", user.getId())
                        .getSingleResult();

                dto.setSort(sort + 1);
            } catch (NoResultException e) {
                dto.setSort(1);
            }
        }

        if (dto.getGroupby() == null || dto.getGroupby().isEmpty())
            dto.setGroupby(GroupBy.SERIES.name() + " " + SortDirection.ASC.name());

        ListEntity newList = null;

        try {
            newList = em.createNamedQuery("getListByNameAndUser", ListEntity.class)
                    .setParameter("name", dto.getName())
                    .setParameter("fkUser", user.getId())
                    .getSingleResult();
        } catch (Exception e) {
            //great, there is no list!
        }

        if (newList != null)
            throw new ConflictException(Errors.LIST_ALREADY_EXISTS);

        list.setName(dto.getName());
        list.setSort(dto.getSort());
        list.setGroupby(dto.getGroupby());
        em.merge(list);
    }

    public void mergeLists(int listFirstId, int listSecondId, UserEntity user) throws NotFoundException, ForbiddenException {
        ListEntity firstList = em.find(ListEntity.class, listFirstId);
        ListEntity secondList = em.find(ListEntity.class, listSecondId);

        if (firstList == null || secondList == null)
            throw new NotFoundException(Errors.LIST_NOT_FOUND);

        if (firstList.getFkUser() != user.getId() || secondList.getFkUser() != user.getId())
            throw new ForbiddenException(Errors.NOT_OWN_LIST);

        List<IssueListEntity> firstRelations = em.createNamedQuery("getILByList", IssueListEntity.class)
                .setParameter("fkList", firstList.getId())
                .getResultList();

        List<IssueListEntity> secondRelations = em.createNamedQuery("getILByList", IssueListEntity.class)
                .setParameter("fkList", secondList.getId())
                .getResultList();

        for (IssueListEntity secondRelation : secondRelations) {
            boolean found = false;

            for (IssueListEntity firstRelation : firstRelations) {
                if (firstRelation.getFkIssue() == secondRelation.getFkIssue()) {
                    firstRelation.setAmount(firstRelation.getAmount() + secondRelation.getAmount());
                    em.merge(firstRelation);
                    em.remove(secondRelation);
                    found = true;
                    continue;
                }
            }

            if (!found) {
                secondRelation.setFkList(firstList.getId());
                em.merge(secondRelation);
            }
        }

        em.remove(secondList);
    }
}
