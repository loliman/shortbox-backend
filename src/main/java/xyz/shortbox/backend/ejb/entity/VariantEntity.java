package xyz.shortbox.backend.ejb.entity;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import xyz.shortbox.backend.dto.BaseDTO;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "Variant", schema = "shortbox")
public class VariantEntity extends BaseEntity {
    private int id;
    private int fkIssue;
    private int fkVariant;

    @Id
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "fk_issue", nullable = false)
    public int getFkIssue() {
        return fkIssue;
    }

    public void setFkIssue(int fkIssue) {
        this.fkIssue = fkIssue;
    }

    @Basic
    @Column(name = "fk_variant", nullable = false)
    public int getFkVariant() {
        return fkVariant;
    }

    public void setFkVariant(int fkVariant) {
        this.fkVariant = fkVariant;
    }

    @Override
    public boolean equals(BaseEntity entity) {
        if (this == entity) return true;
        if (entity == null || getClass() != entity.getClass()) return false;
        VariantEntity that = (VariantEntity) entity;
        return id == that.id &&
                fkIssue == that.fkIssue &&
                fkVariant == that.fkVariant;
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, fkIssue, fkVariant);
    }

    @Override
    public BaseDTO toDTO() {
        throw new NotImplementedException();
    }
}
