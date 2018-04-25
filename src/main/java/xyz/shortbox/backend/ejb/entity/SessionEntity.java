package xyz.shortbox.backend.ejb.entity;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import xyz.shortbox.backend.dto.BaseDTO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "Session", schema = "shortbox")
@NamedQueries({
        @NamedQuery(name = "findSessionByHost", query = "SELECT s FROM SessionEntity s WHERE s.host = :host"),
        @NamedQuery(name = "getSessionsByUser", query = "SELECT s FROM SessionEntity s WHERE s.fkUser = :fkUser"),
        @NamedQuery(name = "getAllSessions", query = "SELECT s FROM SessionEntity s")
})
public class SessionEntity extends BaseEntity {
    private String id;
    private int fkUser;
    private Timestamp lastaccess;
    private String host;
    private byte keep;

    @Id
    @Column(name = "id", nullable = false)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Basic
    @Column(name = "fk_user", nullable = false)
    public int getFkUser() {
        return fkUser;
    }

    public void setFkUser(int fkUser) {
        this.fkUser = fkUser;
    }

    @Basic
    @Column(name = "lastaccess", nullable = false)
    public Timestamp getLastaccess() {
        return lastaccess;
    }

    public void setLastaccess(Timestamp lastaccess) {
        this.lastaccess = lastaccess;
    }

    @Basic
    @Column(name = "host", nullable = false)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Basic
    @Column(name = "keep", nullable = false)
    public byte getKeep() {
        return keep;
    }

    public void setKeep(byte keep) {
        this.keep = keep;
    }

    @Override
    public boolean equals(BaseEntity entity) {
        if (this == entity) return true;
        if (entity == null || getClass() != entity.getClass()) return false;
        SessionEntity that = (SessionEntity) entity;
        return fkUser == that.fkUser &&
                keep == that.keep &&
                Objects.equals(id, that.id) &&
                Objects.equals(lastaccess, that.lastaccess) &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, fkUser, lastaccess, host, keep);
    }

    @Override
    public BaseDTO toDTO() {
        throw new NotImplementedException();
    }
}
