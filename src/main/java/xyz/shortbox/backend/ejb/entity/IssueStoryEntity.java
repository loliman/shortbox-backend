package xyz.shortbox.backend.ejb.entity;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import xyz.shortbox.backend.dto.BaseDTO;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "Issue_Story", schema = "shortbox")
public class IssueStoryEntity extends BaseEntity {
    private int id;
    private int fkIssue;
    private int fkStory;

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
    @Column(name = "fk_story", nullable = false)
    public int getFkStory() {
        return fkStory;
    }

    public void setFkStory(int fkStory) {
        this.fkStory = fkStory;
    }

    @Override
    public boolean equals(BaseEntity entity) {
        if (this == entity) return true;
        if (entity == null || getClass() != entity.getClass()) return false;
        IssueStoryEntity that = (IssueStoryEntity) entity;
        return id == that.id &&
                fkIssue == that.fkIssue &&
                fkStory == that.fkStory;
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, fkIssue, fkStory);
    }

    @Override
    public BaseDTO toDTO() {
        throw new NotImplementedException();
    }
}
