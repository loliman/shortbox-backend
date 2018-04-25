package xyz.shortbox.backend.dto;

import io.swagger.annotations.ApiModel;
import xyz.shortbox.backend.ejb.entity.UserEntity;

import java.sql.Timestamp;
import java.util.Objects;

@ApiModel(value = "User")
public class UserDTO extends BaseDTO {
    private Integer id;
    private String mail;
    private String password;
    private Timestamp registered;
    private Timestamp lastlogin;
    private String usergroup;
    private String state;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Timestamp getRegistered() {
        return registered;
    }

    public void setRegistered(Timestamp registered) {
        this.registered = registered;
    }

    public Timestamp getLastlogin() {
        return lastlogin;
    }

    public void setLastlogin(Timestamp lastlogin) {
        this.lastlogin = lastlogin;
    }

    public String getUsergroup() {
        return usergroup;
    }

    public void setUsergroup(String usergroup) {
        this.usergroup = usergroup;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public boolean equals(BaseDTO dto) {
        if (this == dto) return true;
        if (dto == null || getClass() != dto.getClass()) return false;
        UserDTO userDTO = (UserDTO) dto;
        return Objects.equals(id, userDTO.id) &&
                Objects.equals(mail, userDTO.mail) &&
                Objects.equals(password, userDTO.password) &&
                Objects.equals(registered, userDTO.registered) &&
                Objects.equals(lastlogin, userDTO.lastlogin) &&
                Objects.equals(usergroup, userDTO.usergroup) &&
                Objects.equals(state, userDTO.state);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, mail, password, registered, lastlogin, usergroup, state);
    }

    @Override
    public UserEntity toEntity() {
        UserEntity entity = new UserEntity();

        entity.setId(this.id);
        entity.setUsergroup(this.usergroup);
        entity.setLastlogin(this.lastlogin);
        entity.setMail(this.mail);
        entity.setPassword(this.password);
        entity.setRegistered(this.registered);
        entity.setState(this.state);

        return entity;
    }
}
