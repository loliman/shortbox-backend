package xyz.shortbox.backend.ejb.entity;

import xyz.shortbox.backend.dto.BaseDTO;
import xyz.shortbox.backend.dto.PublisherDTO;
import xyz.shortbox.backend.dto.SeriesDTO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "Series", schema = "shortbox")
public class SeriesEntity extends BaseEntity {
    private int id;
    private String title;
    private int startyear;
    private Integer endyear;
    private int volume;
    private int original;
    private int fkPublisher;
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
    @Column(name = "startyear", nullable = false)
    public int getStartyear() {
        return startyear;
    }

    public void setStartyear(int startyear) {
        this.startyear = startyear;
    }

    @Basic
    @Column(name = "endyear")
    public Integer getEndyear() {
        return endyear;
    }

    public void setEndyear(Integer endyear) {
        this.endyear = endyear;
    }

    @Basic
    @Column(name = "volume", nullable = false)
    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    @Basic
    @Column(name = "original", nullable = false)
    public int getOriginal() {
        return original;
    }

    public void setOriginal(int original) {
        this.original = original;
    }

    @Basic
    @Column(name = "fk_publisher", nullable = false)
    public int getFkPublisher() {
        return fkPublisher;
    }

    public void setFkPublisher(int fkPublisher) {
        this.fkPublisher = fkPublisher;
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
        SeriesEntity that = (SeriesEntity) entity;
        return id == that.id &&
                startyear == that.startyear &&
                volume == that.volume &&
                original == that.original &&
                fkPublisher == that.fkPublisher &&
                Objects.equals(title, that.title) &&
                Objects.equals(endyear, that.endyear) &&
                Objects.equals(created, that.created) &&
                Objects.equals(lastupdate, that.lastupdate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, title, startyear, endyear, volume, original, fkPublisher, created, lastupdate);
    }

    @Override
    public BaseDTO toDTO() {
        SeriesDTO dto = new SeriesDTO();

        dto.setId(this.id);
        dto.setTitle(this.title);
        dto.setEndyear(this.endyear);
        dto.setStartyear(this.startyear);
        dto.setVolume(this.volume);
        dto.setOriginal(this.original == 1);

        PublisherDTO publisher = new PublisherDTO();
        publisher.setId(this.fkPublisher);
        dto.setPublisher(publisher);

        dto.setCreated(this.created);
        dto.setLastupdate(this.lastupdate);

        return dto;
    }
}
