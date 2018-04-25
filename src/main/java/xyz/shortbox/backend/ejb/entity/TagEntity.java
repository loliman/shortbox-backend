package xyz.shortbox.backend.ejb.entity;

import xyz.shortbox.backend.dto.BaseDTO;
import xyz.shortbox.backend.dto.TagDTO;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "Tag", schema = "shortbox")
public class TagEntity extends BaseEntity {
    private int id;
    private String name;
    private int calls;

    @Id
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "name", nullable = false, length = 45)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "calls", nullable = false)
    public int getCalls() {
        return calls;
    }

    public void setCalls(int calls) {
        this.calls = calls;
    }

    @Override
    public boolean equals(BaseEntity entity) {
        if (this == entity) return true;
        if (entity == null || getClass() != entity.getClass()) return false;
        TagEntity tagEntity = (TagEntity) entity;
        return id == tagEntity.id &&
                calls == tagEntity.calls &&
                Objects.equals(name, tagEntity.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, name, calls);
    }

    @Override
    public BaseDTO toDTO() {
        TagDTO dto = new TagDTO();

        dto.setId(this.id);
        dto.setName(this.name);

        return dto;
    }
}
