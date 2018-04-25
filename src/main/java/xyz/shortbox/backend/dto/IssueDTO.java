package xyz.shortbox.backend.dto;

import io.swagger.annotations.ApiModel;
import xyz.shortbox.backend.ejb.entity.BaseEntity;
import xyz.shortbox.backend.ejb.entity.IssueEntity;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApiModel(value = "Issue")
public class IssueDTO extends BaseDTO {
    private Integer id;
    private String title;
    private SeriesDTO series;
    private String number;
    private String format;
    private String variant;
    private Integer limited;
    private String language;
    private Integer pages;
    private Date releasedate;
    private Double price;
    private String currency;
    private String coverurl;
    private Byte dirty;
    private Timestamp created;
    private Timestamp lastupdate;
    private List<StoryDTO> stories = new ArrayList<>();
    private IssueDTO variantOf;
    private List<IssueDTO> variants = new ArrayList<>();
    private List<ListDTO> lists = new ArrayList<>();

    private Integer storyCount;

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

    public SeriesDTO getSeries() {
        return series;
    }

    public void setSeries(SeriesDTO series) {
        this.series = series;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Integer getLimited() {
        return limited;
    }

    public void setLimited(Integer limited) {
        this.limited = limited;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public Date getReleasedate() {
        return releasedate;
    }

    public void setReleasedate(Date releasedate) {
        this.releasedate = releasedate;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCoverurl() {
        return coverurl;
    }

    public void setCoverurl(String coverurl) {
        this.coverurl = coverurl;
    }

    public Byte getDirty() {
        return dirty;
    }

    public void setDirty(Byte dirty) {
        this.dirty = dirty;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public Timestamp getLastupdate() {
        return lastupdate;
    }

    public void setLastupdate(Timestamp lastupdate) {
        this.lastupdate = lastupdate;
    }

    public List<StoryDTO> getStories() {
        return stories;
    }

    public void setStories(List<StoryDTO> stories) {
        this.stories = stories;
    }

    public IssueDTO getVariantOf() {
        return variantOf;
    }

    public void setVariantOf(IssueDTO variantOf) {
        this.variantOf = variantOf;
    }

    public List<IssueDTO> getVariants() {
        return variants;
    }

    public void setVariants(List<IssueDTO> variants) {
        this.variants = variants;
    }

    public List<ListDTO> getLists() {
        return lists;
    }

    public void setLists(List<ListDTO> lists) {
        this.lists = lists;
    }

    public Integer getStoryCount() {
        return storyCount;
    }

    public void setStoryCount(Integer storyCount) {
        this.storyCount = storyCount;
    }

    @Override
    public boolean equals(BaseDTO dto) {
        if (this == dto) return true;
        if (dto == null || getClass() != dto.getClass()) return false;
        IssueDTO issueDTO = (IssueDTO) dto;
        return Objects.equals(id, issueDTO.id) &&
                Objects.equals(title, issueDTO.title) &&
                Objects.equals(series, issueDTO.series) &&
                Objects.equals(number, issueDTO.number) &&
                Objects.equals(format, issueDTO.format) &&
                Objects.equals(variant, issueDTO.variant) &&
                Objects.equals(limited, issueDTO.limited) &&
                Objects.equals(language, issueDTO.language) &&
                Objects.equals(pages, issueDTO.pages) &&
                Objects.equals(releasedate, issueDTO.releasedate) &&
                Objects.equals(price, issueDTO.price) &&
                Objects.equals(currency, issueDTO.currency) &&
                Objects.equals(coverurl, issueDTO.coverurl) &&
                Objects.equals(dirty, issueDTO.dirty) &&
                Objects.equals(created, issueDTO.created) &&
                Objects.equals(lastupdate, issueDTO.lastupdate) &&
                Objects.equals(stories, issueDTO.stories) &&
                Objects.equals(variantOf, issueDTO.variantOf) &&
                Objects.equals(variants, issueDTO.variants) &&
                Objects.equals(lists, issueDTO.lists) &&
                Objects.equals(storyCount, issueDTO.storyCount);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, title, series, number, format, variant, limited, language, pages, releasedate, price, currency, coverurl, dirty, created, lastupdate, stories, variantOf, variants, lists, storyCount);
    }

    @Override
    public BaseEntity toEntity() {
        IssueEntity entitiy = new IssueEntity();

        entitiy.setId(this.id);
        entitiy.setTitle(this.title);
        entitiy.setCoverurl(this.coverurl);
        entitiy.setCurrency(this.currency);
        entitiy.setDirty(this.dirty);
        entitiy.setFkSeries(this.series == null ? 0 : this.series.getId());
        entitiy.setFormat(this.format);
        entitiy.setLanguage(this.language);
        entitiy.setLimited(this.limited);
        entitiy.setNumber(this.number);
        entitiy.setPages(this.pages);
        entitiy.setPrice(this.price);
        entitiy.setReleasedate(this.releasedate);
        entitiy.setVariant(this.variant);
        entitiy.setCreated(this.created);
        entitiy.setLastupdate(this.lastupdate);

        return entitiy;
    }
}
