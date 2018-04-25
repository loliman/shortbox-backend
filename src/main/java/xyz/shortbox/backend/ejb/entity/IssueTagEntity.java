package xyz.shortbox.backend.ejb.entity;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import xyz.shortbox.backend.dto.BaseDTO;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "Issue_Tag", schema = "shortbox")
public class IssueTagEntity extends BaseEntity {
    private int id;
    private int fkIssue;
    private int fkTag;

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
    @Column(name = "fk_tag", nullable = false)
    public int getFkTag() {
        return fkTag;
    }

    public void setFkTag(int fkTag) {
        this.fkTag = fkTag;
    }

    @Override
    public boolean equals(BaseEntity entity) {
        if (this == entity) return true;
        if (entity == null || getClass() != entity.getClass()) return false;
        IssueTagEntity that = (IssueTagEntity) entity;
        return id == that.id &&
                fkIssue == that.fkIssue &&
                fkTag == that.fkTag;
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, fkIssue, fkTag);
    }

    @Override
    public BaseDTO toDTO() {
        throw new NotImplementedException();
    }
}
