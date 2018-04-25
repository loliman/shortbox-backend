package xyz.shortbox.backend.ejb.entity;

import xyz.shortbox.backend.dto.BaseDTO;
import xyz.shortbox.backend.dto.PublisherDTO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "Publisher", schema = "shortbox")
public class PublisherEntity extends BaseEntity {
    private int id;
    private String name;
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
    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        PublisherEntity that = (PublisherEntity) entity;
        return id == that.id &&
                Objects.equals(name, that.name) &&
                Objects.equals(created, that.created) &&
                Objects.equals(lastupdate, that.lastupdate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, name, created, lastupdate);
    }

    @Override
    public BaseDTO toDTO() {
        PublisherDTO dto = new PublisherDTO();

        dto.setId(this.id);
        dto.setName(this.name);
        dto.setCreated(this.created);
        dto.setLastupdate(this.lastupdate);

        return dto;
    }
}
