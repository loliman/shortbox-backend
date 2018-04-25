package xyz.shortbox.backend.ejb.entity;

import xyz.shortbox.backend.dto.BaseDTO;
import xyz.shortbox.backend.dto.ListDTO;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "List", schema = "shortbox")
@NamedQueries({
        @NamedQuery(name = "getListsByUser", query = "SELECT l FROM ListEntity l WHERE fkUser = :fkUser")
})
public class ListEntity extends BaseEntity {
    private int id;
    private String name;
    private int sort;
    private String groupby;
    private int fkUser;

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
    @Column(name = "sort", nullable = false)
    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    @Basic
    @Column(name = "groupby", nullable = false)
    public String getGroupby() {
        return groupby;
    }

    public void setGroupby(String groupby) {
        this.groupby = groupby;
    }

    @Basic
    @Column(name = "fk_user", nullable = false)
    public int getFkUser() {
        return fkUser;
    }

    public void setFkUser(int fkUser) {
        this.fkUser = fkUser;
    }

    @Override
    public boolean equals(BaseEntity entity) {
        if (this == entity) return true;
        if (entity == null || getClass() != entity.getClass()) return false;
        ListEntity that = (ListEntity) entity;
        return id == that.id &&
                sort == that.sort &&
                fkUser == that.fkUser &&
                Objects.equals(name, that.name) &&
                Objects.equals(groupby, that.groupby);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, name, sort, groupby, fkUser);
    }

    @Override
    public BaseDTO toDTO() {
        ListDTO dto = new ListDTO();

        dto.setId(this.id);
        dto.setName(this.name);
        dto.setSort(this.sort);
        dto.setGroupby(this.groupby);

        return dto;
    }
}
