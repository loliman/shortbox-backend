package xyz.shortbox.backend.dto;

import io.swagger.annotations.ApiModel;
import xyz.shortbox.backend.ejb.entity.ListEntity;

import java.util.Objects;

@ApiModel(value = "List")
public class ListDTO extends BaseDTO {
    private Integer id;
    private String name;
    private Integer sort;
    private String groupby;

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

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getGroupby() {
        return groupby;
    }

    public void setGroupby(String groupby) {
        this.groupby = groupby;
    }

    @Override
    public boolean equals(BaseDTO dto) {
        if (this == dto) return true;
        if (dto == null || getClass() != dto.getClass()) return false;
        ListDTO listDTO = (ListDTO) dto;
        return Objects.equals(id, listDTO.id) &&
                Objects.equals(name, listDTO.name) &&
                Objects.equals(sort, listDTO.sort) &&
                Objects.equals(groupby, listDTO.groupby);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, name, sort, groupby);
    }

    @Override
    public ListEntity toEntity() {
        ListEntity entitiy = new ListEntity();

        entitiy.setId(this.id);
        entitiy.setName(this.name);
        entitiy.setSort(this.sort);
        entitiy.setGroupby(this.groupby);

        return entitiy;
    }
}
