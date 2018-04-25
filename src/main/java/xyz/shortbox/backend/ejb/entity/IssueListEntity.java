package xyz.shortbox.backend.ejb.entity;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import xyz.shortbox.backend.dto.BaseDTO;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "Issue_List", schema = "shortbox")
@NamedQueries({
        @NamedQuery(name = "getILByList", query = "SELECT il FROM IssueListEntity il WHERE il.fkList = :fkList")
})
public class IssueListEntity extends BaseEntity {
    private int id;
    private int fkIssue;
    private int fkList;
    private int amount;

    @Id
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "fk_issue", nullable = false)
    public int getFkIssue() {
        return fkIssue;
    }

    public void setFkIssue(int fkIssue) {
        this.fkIssue = fkIssue;
    }

    @Basic
    @Column(name = "fk_list", nullable = false)
    public int getFkList() {
        return fkList;
    }

    public void setFkList(int fkList) {
        this.fkList = fkList;
    }

    @Basic
    @Column(name = "amount", nullable = false)
    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean equals(BaseEntity entity) {
        if (this == entity) return true;
        if (entity == null || getClass() != entity.getClass()) return false;
        IssueListEntity that = (IssueListEntity) entity;
        return id == that.id &&
                fkIssue == that.fkIssue &&
                fkList == that.fkList &&
                amount == that.amount;
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, fkIssue, fkList, amount);
    }

    @Override
    public BaseDTO toDTO() {
        throw new NotImplementedException();
    }
}
