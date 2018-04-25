package xyz.shortbox.backend.ejb.entity;

import xyz.shortbox.backend.dto.BaseDTO;
import xyz.shortbox.backend.dto.StoryDTO;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "Story", schema = "shortbox")
@NamedQueries({
        @NamedQuery(name = "Issue_Story.countForIssue", query = "SELECT COUNT(t) FROM IssueStoryEntity t WHERE t.fkIssue = :fkIssue"),
        @NamedQuery(name = "findAllStoriesForIssue", query = "SELECT s FROM StoryEntity s WHERE id IN (SELECT t.fkStory FROM IssueStoryEntity t WHERE t.fkIssue = :fkIssue)"),
        @NamedQuery(name = "getIssuesForStoryWithoutMain", query = "SELECT i.id, i.fkSeries, i.number FROM IssueEntity i WHERE i.id IN (SELECT t.fkIssue FROM IssueStoryEntity t WHERE t.fkStory= :fkStory) AND i.id != :fkIssue AND i.id NOT IN (SELECT v.fkVariant FROM VariantEntity v)")
})
public class StoryEntity extends BaseEntity {
    private int id;
    private String title;
    private Integer number;
    private String additionalinfo;

    @Id
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "title", nullable = false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Basic
    @Column(name = "number")
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    @Basic
    @Column(name = "additionalinfo")
    public String getAdditionalinfo() {
        return additionalinfo;
    }

    public void setAdditionalinfo(String additionalinfo) {
        this.additionalinfo = additionalinfo;
    }

    @Override
    public boolean equals(BaseEntity entity) {
        if (this == entity) return true;
        if (entity == null || getClass() != entity.getClass()) return false;
        StoryEntity that = (StoryEntity) entity;
        return id == that.id &&
                Objects.equals(title, that.title) &&
                Objects.equals(number, that.number) &&
                Objects.equals(additionalinfo, that.additionalinfo);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, title, number, additionalinfo);
    }

    @Override
    public BaseDTO toDTO() {
        StoryDTO dto = new StoryDTO();

        dto.setId(this.id);
        dto.setTitle(this.title);
        dto.setNumber(this.number);
        dto.setAdditionalinfo(this.additionalinfo);

        return dto;
    }
}
