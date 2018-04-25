package xyz.shortbox.backend.dto;

import io.swagger.annotations.ApiModel;
import xyz.shortbox.backend.ejb.entity.SeriesEntity;

import java.sql.Timestamp;
import java.util.Objects;

@ApiModel(value = "Series")
public class SeriesDTO extends BaseDTO {
    private Integer id;
    private String title;
    private Integer startyear;
    private Integer endyear;
    private Integer volume;
    private Boolean original;
    private PublisherDTO publisher;
    private Timestamp created;
    private Timestamp lastupdate;

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

    public Integer getStartyear() {
        return startyear;
    }

    public void setStartyear(Integer startyear) {
        this.startyear = startyear;
    }

    public Integer getEndyear() {
        return endyear;
    }

    public void setEndyear(Integer endyear) {
        this.endyear = endyear;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public Boolean isOriginal() {
        return original;
    }

    public void setOriginal(Boolean original) {
        this.original = original;
    }

    public PublisherDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(PublisherDTO publisher) {
        this.publisher = publisher;
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

    @Override
    public boolean equals(BaseDTO dto) {
        if (this == dto) return true;
        if (dto == null || getClass() != dto.getClass()) return false;
        SeriesDTO seriesDTO = (SeriesDTO) dto;
        return Objects.equals(id, seriesDTO.id) &&
                Objects.equals(title, seriesDTO.title) &&
                Objects.equals(startyear, seriesDTO.startyear) &&
                Objects.equals(endyear, seriesDTO.endyear) &&
                Objects.equals(volume, seriesDTO.volume) &&
                Objects.equals(original, seriesDTO.original) &&
                Objects.equals(publisher, seriesDTO.publisher) &&
                Objects.equals(created, seriesDTO.created) &&
                Objects.equals(lastupdate, seriesDTO.lastupdate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, title, startyear, endyear, volume, original, publisher, created, lastupdate);
    }

    @Override
    public SeriesEntity toEntity() {
        SeriesEntity entitiy = new SeriesEntity();

        entitiy.setId(this.id);
        entitiy.setTitle(this.title);
        entitiy.setEndyear(this.endyear);
        entitiy.setStartyear(this.startyear);
        entitiy.setVolume(this.volume);
        entitiy.setOriginal(this.original ? 1 : 0);
        entitiy.setFkPublisher(this.publisher == null ? 0 : this.publisher.getId());
        entitiy.setCreated(this.created);
        entitiy.setLastupdate(this.lastupdate);

        return entitiy;
    }
}
