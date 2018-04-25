package xyz.shortbox.backend.dto;

import io.swagger.annotations.ApiModel;
import xyz.shortbox.backend.ejb.entity.StoryEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApiModel(value = "Story")
public class StoryDTO extends BaseDTO {
    private Integer id;
    private String title;
    private Integer number;
    private String additionalinfo;
    private Integer issueCount;
    private List<IssueDTO> issues = new ArrayList<>();
    private IssueDTO originalIssue;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getAdditionalinfo() {
        return additionalinfo;
    }

    public void setAdditionalinfo(String additionalinfo) {
        this.additionalinfo = additionalinfo;
    }

    public Integer getIssueCount() {
        return issueCount;
    }

    public void setIssueCount(Integer issueCount) {
        this.issueCount = issueCount;
    }

    public List<IssueDTO> getIssues() {
        return issues;
    }

    public void setIssues(List<IssueDTO> issues) {
        this.issues = issues;
    }

    public IssueDTO getOriginalIssue() {
        return originalIssue;
    }

    public void setOriginalIssue(IssueDTO originalIssue) {
        this.originalIssue = originalIssue;
    }

    @Override
    public boolean equals(BaseDTO dto) {
        if (this == dto) return true;
        if (dto == null || getClass() != dto.getClass()) return false;
        StoryDTO storyDTO = (StoryDTO) dto;
        return Objects.equals(id, storyDTO.id) &&
                Objects.equals(title, storyDTO.title) &&
                Objects.equals(number, storyDTO.number) &&
                Objects.equals(additionalinfo, storyDTO.additionalinfo) &&
                Objects.equals(issueCount, storyDTO.issueCount) &&
                Objects.equals(issues, storyDTO.issues) &&
                Objects.equals(originalIssue, storyDTO.originalIssue);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, title, number, additionalinfo, issueCount, issues, originalIssue);
    }

    @Override
    public StoryEntity toEntity() {
        StoryEntity entitiy = new StoryEntity();

        entitiy.setId(this.id);
        entitiy.setTitle(this.title);
        entitiy.setNumber(this.number);
        entitiy.setAdditionalinfo(this.additionalinfo);

        return entitiy;
    }
}
