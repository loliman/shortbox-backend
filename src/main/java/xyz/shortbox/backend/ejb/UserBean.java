package xyz.shortbox.backend.ejb;

import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.config.TransportStrategy;
import xyz.shortbox.backend.config.Configuration;
import xyz.shortbox.backend.dto.UserDTO;
import xyz.shortbox.backend.ejb.entity.IssueListEntity;
import xyz.shortbox.backend.ejb.entity.ListEntity;
import xyz.shortbox.backend.ejb.entity.SessionEntity;
import xyz.shortbox.backend.ejb.entity.UserEntity;
import xyz.shortbox.backend.enumeration.UserGroup;
import xyz.shortbox.backend.enumeration.UserState;
import xyz.shortbox.backend.error.Errors;
import xyz.shortbox.backend.exception.ConflictException;
import xyz.shortbox.backend.exception.ForbiddenException;
import xyz.shortbox.backend.exception.NotFoundException;

import javax.ejb.Stateless;
import javax.mail.Message;
import javax.persistence.NoResultException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Contains all Methods that are used to retrieve and manipulate User data.
 * <p>
 * The data of all returned and modified objects will be retrieved directly from the underlying database.
 * Every manipulated object will be persisted in the underlying database directly.
 * </p>
 */
@Stateless
public class UserBean extends BaseBean {

    public static String JNDI_NAME = "java:global/api/UserBean!xyz.shortbox.backend.ejb.UserBean";

    /**
     * Returnes the given {@code UserEntity} as {@code UserDTO}.
     * <p>
     * ID, password, last login and state will be removed from the {@link UserDTO}.
     * </p>
     *
     * @param user the {@link UserEntity}, that should be converted.
     * @return the converted {@code UserDTO}.
     */
    public UserDTO getUser(UserEntity user) {
        UserDTO dto = (UserDTO) user.toDTO();

        dto.setId(null);
        dto.setPassword(null);
        dto.setLastlogin(null);
        dto.setState(null);

        return dto;
    }

    /**
     * Creates a new {@code UserEntity}.
     * <p>
     * The {@link UserEntity} will be in state {@code #UserState.REGISTER} and group
     * {@code #UserGroup.USER}.
     * Also the token will be set and a mail with the token will be sent to the registered mail.
     * </p>
     *
     * @param mail     the mail of the new {@code UserEntity}.
     * @param password the password of the new {@code UserEntity}.
     * @return the generated token, that was sent by mail.
     * @throws ConflictException if an {@code UserEntity} with the same mail already exists.
     */
    public String register(String mail, String password) throws ConflictException {
        String token = keyGen.nextString();
        createUser(mail, password, token);
        sendMail(mail, "Noch ein Schritt bis zur Shortbox...", "Dein token: " + token);
        return Configuration.TEST_MODE ? token : null;
    }

    /**
     * Finishes the {@code UserEntity} registration process.
     * <p>
     * Sets the {@link UserEntity} state to {@code #UserState.ACTIVE} and removes the token.
     * Also a welcome mail will be send to the registered mail.
     * </p>
     *
     * @param token the token, that was sent by mail.
     * @throws ConflictException if the {@code UserEntity} is not in state {@code #UserState.REGISTER}
     * @throws NotFoundException if no {@code UserEntity} for the given token is found.
     */
    public void finishRegistration(String token) throws ConflictException, NotFoundException {
        UserEntity user = findUserByToken(token);

        checkIfUserIsInState(user, UserState.REGISTER);

        updateUser(user, user.getPassword(), null, UserState.ACTIVE);
        sendMail(user.getMail(), "Willkommen!", "Registrierung abgeschlossen.");
    }

    /**
     * Changes the given {@code UserEntity} password.
     * <p>
     * A mail will be send to the registered mail afterwards.
     * </p>
     *
     * @param user        the {@code UserEntity}, that should be changed.
     * @param oldPassword the old password.
     * @param newPassword the new password.
     * @throws ConflictException  if the {@code UserEntity} is not in {@code #UserState.ACTIVE}.
     * @throws ForbiddenException if the old and current password do not match.
     */
    public void changePassword(UserEntity user, String oldPassword, String newPassword) throws ConflictException, ForbiddenException {
        changeUserPassword(user, oldPassword, newPassword);
        sendMail(user.getMail(), "Passwort zurück gesetzt", "Passwort erfolgreich zurück gesetzt.");
    }

    /**
     * Resets the given {@code UserEntity} password.
     * <p>
     * A mail will be send to the registered mail afterwards.
     * lso the {@link UserEntity} will be set to {@code #UserState.ACTIVE}.
     * </p>
     *
     * @param token       the token, that should be searched for.
     * @param newPassword the new password.
     * @throws NotFoundException if no {@code UserEntity} for the given token is found.
     * @throws ConflictException if the {@code UserEntity} is not in state {@code #UserState.FORGOT_PW}.
     */
    public void resetPassword(String token, String newPassword) throws NotFoundException, ConflictException {
        UserEntity user = findUserByToken(token);

        checkIfUserIsInState(user, UserState.FORGOT_PW);
        resetUserPassword(user, newPassword);
        sendMail(user.getMail(), "Passwortänderung", "Passwort erfolgreich geändert.");
    }

    /**
     * Starts the forgot password process.
     * <p>
     * A token will be generated a mail will be sent to the registered mail.
     * Also the {@link UserEntity} will be set to {@code #UserState.FORGOT_PW}.
     * </p>
     *
     * @param mail the mail of the {@code UserEntity}, that should be reset.
     * @return the generated token.
     * @throws NotFoundException if no {@code UserEntity} for the given mail was found.
     * @throws ConflictException if the {@code UserEntity} is not in state {@code #UserState.ACTIVE}.
     */
    public String forgotPassword(String mail) throws NotFoundException, ConflictException {
        UserEntity user = findUserByMail(mail);

        checkIfUserIsInState(user, UserState.ACTIVE);

        String token = keyGen.nextString();
        updateUser(user, user.getPassword(), token, UserState.FORGOT_PW);

        sendMail(user.getMail(), "Passwort vergessen", "Hast du dein Passwort vergessen? Dein token " + token);

        return token;
    }

    /**
     * Removes a {@code UserEntity}.
     * <p>
     * Also all related {@link SessionEntity}, {@link ListEntity} and {@link IssueListEntity} will be removed.
     * A mail will be send to the registered mail afterwards.
     * </p>
     *
     * @param user     the {@link UserEntity}, that should be removed.
     * @param password the password of the {@code UserEntity}.
     * @throws ConflictException  if the {@code UserEntity} is not in state {@code #UserState.ACTIVE}.
     * @throws ForbiddenException if the password and the current password do not match.
     */
    public void deleteUser(UserEntity user, String password) throws ConflictException, ForbiddenException {
        checkIfUserIsInState(user, UserState.ACTIVE);
        checkIfAuthKeysMatch(password, user.getPassword());

        deleteUser(user);

        sendMail(user.getMail(), "User gelöscht", "Der Benutzer wurde erfolgreich gelöscht.");
    }

    /**
     * Creates a new {@code UserEntity}.
     * <p>
     * The {@link UserEntity} {@code #UserState} will be REGISTER.
     * The {@code UserEntity} {@code #UserGroup} will be USER.
     * </p>
     *
     * @param mail     the mail.
     * @param password the password.
     * @param token    the token.
     * @throws ConflictException if a {@code UserEntity} with the same mail already exists.
     */
    private void createUser(String mail, String password, String token) throws ConflictException {
        UserEntity user = null;

        try {
            user = findUserByMail(mail);
        } catch (Exception e) {
            //great, there is no user!
        }

        if (user != null)
            throw new ConflictException(Errors.USER_ALREADY_EXISTS);

        user = new UserEntity();
        user.setMail(mail);
        user.setPassword(password);
        user.setState(UserState.REGISTER.name());
        user.setUsergroup(UserGroup.USER.name());
        user.setToken(token);
        user.setRegistered(new Timestamp(System.currentTimeMillis()));

        em.persist(user);
    }

    /**
     * Returns a {@code UserEntity}, corresponding to the given token
     *
     * @param token the token of the {@link UserEntity}.
     * @return the {@code UserEntity} corresponding to the given token.
     * @throws NotFoundException if there is no {@code UserEntity} corresponding to the given token.
     */
    private UserEntity findUserByToken(String token) throws NotFoundException {
        UserEntity user;

        try {
            user = em.createNamedQuery("getUserByToken", UserEntity.class)
                    .setParameter("token", token)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new NotFoundException(Errors.USER_NOT_FOUND);
        }

        return user;
    }

    /**
     * Returns a {@code UserEntity}, corresponding to the given mail
     *
     * @param mail the mail of the {@link UserEntity}.
     * @return the {@code UserEntity} corresponding to the given mail.
     * @throws NotFoundException if there is no {@code UserEntity} corresponding to the given mail.
     */
    private UserEntity findUserByMail(String mail) throws NotFoundException {
        try {
            return em.createNamedQuery("getUserByMail", UserEntity.class)
                    .setParameter("mail", mail)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new NotFoundException(Errors.USER_NOT_FOUND);
        }
    }

    /**
     * Removes a {@code UserEntity}.
     *
     * @param user the {@link UserEntity}, that should be removed.
     */
    private void deleteUser(UserEntity user) {
        deleteAllSessionsForUser(user);
        deleteAllListsForUser(user);

        em.remove(em.contains(user) ? user : em.merge(user));
    }

    /**
     * Removes all {@code SessionEntity} for given {@code UserEntity}.
     *
     * @param user the {@link UserEntity} all {@link SessionEntity} should be removed for.
     */
    private void deleteAllSessionsForUser(UserEntity user) {
        List<SessionEntity> sessions = em.createNamedQuery("getSessionsByUser", SessionEntity.class)
                .setParameter("fkUser", user.getId())
                .getResultList();

        sessions.forEach(session -> em.remove(session));
    }

    /**
     * Removes all {@code ListEntity} for given {@code UserEntity}.
     * <p>
     * For each {@link ListEntity} all {@link xyz.shortbox.backend.ejb.entity.IssueListEntity} will be
     * removed.
     * </p>
     *
     * @param user the {@link UserEntity} all {@code ListEntity} should be removed for.
     */
    private void deleteAllListsForUser(UserEntity user) {
        List<ListEntity> lists = em.createNamedQuery("getListsByUser", ListEntity.class)
                .setParameter("fkUser", user.getId())
                .getResultList();

        lists.forEach(list -> {
            removeAllListRelations(list);
            em.remove(list);
        });
    }

    /**
     * Removes all {@code IssueListEntity} for given {@code ListEntity}.
     *
     * @param list the {@link ListEntity} all {@link IssueListEntity} should be removed for.
     */
    private void removeAllListRelations(ListEntity list) {
        List<IssueListEntity> relations = em.createNamedQuery("getILByList", IssueListEntity.class)
                .setParameter("fkList", list.getId())
                .getResultList();

        relations.forEach(relation -> em.remove(relation));
    }

    /**
     * Updates a a {@code UserEntity} to the given parameters.
     *
     * @param user        the {@link UserEntity}, that should be updated.
     * @param newPassword the new password.
     * @param token       the new token value.
     * @param state       the new {@code #UserState}.
     */
    private void updateUser(UserEntity user, String newPassword, String token, UserState state) {
        user.setPassword(newPassword);
        user.setToken(token);
        user.setState(state.name());

        if (!em.contains(user))
            em.merge(user);
    }

    /**
     * Changes a {@code UserEntity} password.
     * <p>
     * Also sets the {@link UserEntity} to {@code #UserState.ACTIVE} and token to null.
     * </p>
     *
     * @param user        the {@code UserEntity}.
     * @param oldPassword the current password.
     * @param newPassword the new password.
     * @throws ConflictException  if the {@code UserEntity} is not in {@code #UserState.ACTIVE}.
     * @throws ForbiddenException if the given and current password do not match.
     */
    private void changeUserPassword(UserEntity user, String oldPassword, String newPassword) throws ConflictException, ForbiddenException {
        checkIfUserIsInState(user, UserState.ACTIVE);
        checkIfAuthKeysMatch(oldPassword, user.getPassword());

        updateUser(user, newPassword, null, UserState.ACTIVE);
    }

    /**
     * Checks if the {@code UserEntity} is in the right state.
     *
     * @param user  the {@link UserEntity}.
     * @param state the state, that the {@code UserEntity} should be in.
     * @throws ConflictException if the states do not match.
     */
    private void checkIfUserIsInState(UserEntity user, UserState state) throws ConflictException {
        if (!user.getState().equals(state.name()))
            throw new ConflictException(Errors.USER_IS_IN_WRONG_STATE);
    }

    /**
     * Resets the {@code UserEntity} password.
     * <p>
     * For that the {@link UserEntity} will be set to {@code #UserState.ACTIVE}.
     * Also the token will be set to null.
     * </p>
     *
     * @param user        the {@code UserEntity}, that should be updated.
     * @param newPassword the new password.
     */
    private void resetUserPassword(UserEntity user, String newPassword) {
        updateUser(user, newPassword, null, UserState.ACTIVE);
    }

    /**
     * Compares two auth keys.
     * <p>
     * Fails if given parameters do not match.
     * </p>
     *
     * @param key1 first key.
     * @param key2 second key.
     * @throws ForbiddenException if keys do not match.
     */
    private void checkIfAuthKeysMatch(String key1, String key2) throws ForbiddenException {
        if (!key1.equals(key2))
            throw new ForbiddenException(Errors.KEYS_DO_NOT_MATCH);
    }

    /**
     * Sends a mail to the given receiver.
     * <p>
     * The mail will be sent from info@shortbox.xyz.
     * The mail provider used is Google via SMTP.
     * </p>
     *
     * @param to      the mail receiver.
     * @param subject the mail subject.
     * @param msg     the message body.
     */
    private void sendMail(String to, String subject, String msg) {
        try {
            if (!Configuration.TEST_MODE || (Configuration.TEST_MODE && !Configuration.DISABLE_MAIL)) {
                Email email = new Email();

                String host = Configuration.MAIL_HOST;
                Integer port = Configuration.MAIL_PORT;
                String from = Configuration.MAIL_USER;
                String pass = Configuration.MAIL_PASSWORD;

                email.setFromAddress("info@shortbox.xyz", "info@shortbox.xyz");
                email.setSubject(subject);
                email.addRecipient("", to, Message.RecipientType.TO);
                email.setText(msg);

                new Mailer(host, port, from, pass, TransportStrategy.SMTP_TLS).sendMail(email);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}