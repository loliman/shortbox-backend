package xyz.shortbox.backend.ejb.entity;

import xyz.shortbox.backend.dto.BaseDTO;
import xyz.shortbox.backend.dto.IssueDTO;
import xyz.shortbox.backend.dto.SeriesDTO;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "Issue", schema = "shortbox")
@NamedQueries({
        @NamedQuery(name = "findVariantsForIssue", query = "SELECT i FROM IssueEntity i WHERE i.id IN (SELECT v.fkVariant FROM VariantEntity v WHERE v.fkIssue = :fkIssue AND v.fkVariant != :fkVariant)"),
        @NamedQuery(name = "findListsForIssue", query = "SELECT l FROM ListEntity l WHERE l.id IN (SELECT il.fkList FROM IssueListEntity il WHERE il.fkIssue= :fkIssue)"),
        @NamedQuery(name = "findMainIssueForVariant", query = "SELECT i FROM IssueEntity i WHERE i.id IN (SELECT v.fkIssue FROM VariantEntity v WHERE v.fkVariant = :fkVariant)")
})
public class IssueEntity extends BaseEntity {
    private int id;
    private String title;
    private int fkSeries;
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
    @Column(name = "fk_series", nullable = false)
    public int getFkSeries() {
        return fkSeries;
    }

    public void setFkSeries(int fkSeries) {
        this.fkSeries = fkSeries;
    }

    @Basic
    @Column(name = "number", nullable = false)
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Basic
    @Column(name = "format", nullable = false)
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Basic
    @Column(name = "variant", nullable = false)
    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    @Basic
    @Column(name = "limited")
    public Integer getLimited() {
        return limited;
    }

    public void setLimited(Integer limited) {
        this.limited = limited;
    }

    @Basic
    @Column(name = "language")
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Basic
    @Column(name = "pages")
    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    @Basic
    @Column(name = "releasedate")
    public Date getReleasedate() {
        return releasedate;
    }

    public void setReleasedate(Date releasedate) {
        this.releasedate = releasedate;
    }

    @Basic
    @Column(name = "price")
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    @Basic
    @Column(name = "currency", length = 3)
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Basic
    @Column(name = "coverurl")
    public String getCoverurl() {
        return coverurl;
    }

    public void setCoverurl(String coverurl) {
        this.coverurl = coverurl;
    }

    @Basic
    @Column(name = "dirty")
    public Byte getDirty() {
        return dirty;
    }

    public void setDirty(Byte dirty) {
        this.dirty = dirty;
    }

    @Basic
    @Column(name = "created", nullable = false)
    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    @Basic
    @Column(name = "lastupdate")
    public Timestamp getLastupdate() {
        return lastupdate;
    }

    public void setLastupdate(Timestamp lastupdate) {
        this.lastupdate = lastupdate;
    }

    @Override
    public boolean equals(BaseEntity entity) {
        if (this == entity) return true;
        if (entity == null || getClass() != entity.getClass()) return false;
        IssueEntity that = (IssueEntity) entity;
        return id == that.id &&
                fkSeries == that.fkSeries &&
                Objects.equals(title, that.title) &&
                Objects.equals(number, that.number) &&
                Objects.equals(format, that.format) &&
                Objects.equals(variant, that.variant) &&
                Objects.equals(limited, that.limited) &&
                Objects.equals(language, that.language) &&
                Objects.equals(pages, that.pages) &&
                Objects.equals(releasedate, that.releasedate) &&
                Objects.equals(price, that.price) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(coverurl, that.coverurl) &&
                Objects.equals(dirty, that.dirty) &&
                Objects.equals(created, that.created) &&
                Objects.equals(lastupdate, that.lastupdate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, title, fkSeries, number, format, variant, limited, language, pages, releasedate, price, currency, coverurl, dirty, created, lastupdate);
    }

    @Override
    public BaseDTO toDTO() {
        IssueDTO dto = new IssueDTO();

        dto.setId(this.id);
        dto.setTitle(this.title);
        dto.setCoverurl(this.coverurl);
        dto.setCurrency(this.currency);
        dto.setDirty(this.dirty);

        SeriesDTO series = new SeriesDTO();
        series.setId(this.fkSeries);
        dto.setSeries(series);

        dto.setFormat(this.format);
        dto.setLanguage(this.language);
        dto.setLimited(this.limited);
        dto.setNumber(this.number);
        dto.setPages(this.pages);
        dto.setPrice(this.price);
        dto.setReleasedate(this.releasedate);
        dto.setVariant(this.variant);
        dto.setCreated(this.created);
        dto.setLastupdate(this.lastupdate);

        return dto;
    }
}
