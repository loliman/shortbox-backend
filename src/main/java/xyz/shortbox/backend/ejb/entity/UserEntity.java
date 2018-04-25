package xyz.shortbox.backend.ejb.entity;

import xyz.shortbox.backend.dto.BaseDTO;
import xyz.shortbox.backend.dto.UserDTO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "User", schema = "shortbox")
@NamedQueries({
        @NamedQuery(name = "findUserByMailAndPassword", query = "SELECT u FROM UserEntity u WHERE u.mail = :mail AND u.password = :password"),
        @NamedQuery(name = "getUserByToken", query = "SELECT u FROM UserEntity u WHERE u.token = :token"),
        @NamedQuery(name = "getUserByMail", query = "SELECT u FROM UserEntity u WHERE u.mail = :mail")
})
public class UserEntity extends BaseEntity {
    private int id;
    private String mail;
    private String password;
    private Timestamp registered;
    private Timestamp lastlogin;
    private String usergroup;
    private String state;
    private String token;

    @Id
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "mail", nullable = false)
    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    @Basic
    @Column(name = "password", nullable = false)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Basic
    @Column(name = "registered", nullable = false)
    public Timestamp getRegistered() {
        return registered;
    }

    public void setRegistered(Timestamp registered) {
        this.registered = registered;
    }

    @Basic
    @Column(name = "lastlogin")
    public Timestamp getLastlogin() {
        return lastlogin;
    }

    public void setLastlogin(Timestamp lastlogin) {
        this.lastlogin = lastlogin;
    }

    @Basic
    @Column(name = "usergroup", nullable = false)
    public String getUsergroup() {
        return usergroup;
    }

    public void setUsergroup(String usergroup) {
        this.usergroup = usergroup;
    }

    @Basic
    @Column(name = "state", nullable = false)
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Basic
    @Column(name = "token")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(BaseEntity entity) {
        if (this == entity) return true;
        if (entity == null || getClass() != entity.getClass()) return false;
        UserEntity that = (UserEntity) entity;
        return id == that.id &&
                Objects.equals(mail, that.mail) &&
                Objects.equals(password, that.password) &&
                Objects.equals(registered, that.registered) &&
                Objects.equals(lastlogin, that.lastlogin) &&
                Objects.equals(usergroup, that.usergroup) &&
                Objects.equals(state, that.state) &&
                Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, mail, password, registered, lastlogin, usergroup, state, token);
    }

    @Override
    public BaseDTO toDTO() {
        UserDTO dto = new UserDTO();

        dto.setId(this.id);
        dto.setUsergroup(this.usergroup);
        dto.setLastlogin(this.lastlogin);
        dto.setMail(this.mail);
        dto.setPassword(this.password);
        dto.setRegistered(this.registered);
        dto.setState(this.state);

        return dto;
    }
}
