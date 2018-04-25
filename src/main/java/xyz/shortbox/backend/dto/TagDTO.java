package xyz.shortbox.backend.dto;

import io.swagger.annotations.ApiModel;
import xyz.shortbox.backend.ejb.entity.BaseEntity;
import xyz.shortbox.backend.ejb.entity.TagEntity;

import java.util.Objects;

@ApiModel(value = "Tag")
public class TagDTO extends BaseDTO {
    private Integer id;
    private String name;

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

    @Override
    public boolean equals(BaseDTO dto) {
        if (this == dto) return true;
        if (dto == null || getClass() != dto.getClass()) return false;
        TagDTO tagDTO = (TagDTO) dto;
        return Objects.equals(id, tagDTO.id) &&
                Objects.equals(name, tagDTO.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, name);
    }

    @Override
    public BaseEntity toEntity() {
        TagEntity entity = new TagEntity();

        entity.setId(this.id);
        entity.setName(this.name);

        return entity;
    }
}
