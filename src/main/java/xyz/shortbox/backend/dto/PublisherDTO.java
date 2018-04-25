package xyz.shortbox.backend.dto;

import io.swagger.annotations.ApiModel;
import xyz.shortbox.backend.ejb.entity.PublisherEntity;

import java.sql.Timestamp;
import java.util.Objects;

@ApiModel(value = "Publisher")
public class PublisherDTO extends BaseDTO {
    private Integer id;
    private String name;
    private Timestamp created;
    private Timestamp lastupdate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        PublisherDTO that = (PublisherDTO) dto;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(created, that.created) &&
                Objects.equals(lastupdate, that.lastupdate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, name, created, lastupdate);
    }

    @Override
    public PublisherEntity toEntity() {
        PublisherEntity entitiy = new PublisherEntity();

        entitiy.setId(this.id);
        entitiy.setName(this.name);
        entitiy.setCreated(this.created);
        entitiy.setLastupdate(this.lastupdate);

        return entitiy;
    }
}
