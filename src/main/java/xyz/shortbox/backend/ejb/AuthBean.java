package xyz.shortbox.backend.ejb;


import xyz.shortbox.backend.config.Configuration;
import xyz.shortbox.backend.ejb.entity.SessionEntity;
import xyz.shortbox.backend.ejb.entity.UserEntity;
import xyz.shortbox.backend.enumeration.UserState;
import xyz.shortbox.backend.error.Errors;
import xyz.shortbox.backend.exception.ConflictException;
import xyz.shortbox.backend.exception.ForbiddenException;
import xyz.shortbox.backend.exception.NotFoundException;
import xyz.shortbox.backend.exception.UnauthorizedException;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

/**
 * Contains all Methods that are used to retrieve and manipulate Session data.
 * <p>
 * The data of all returned and modified objects will be retrieved directly from the underlying database.
 * Methods in this beans SHOULD never return any Objects except Strings.
 * Every manipulated object will be persisted in the underlying database directly.
 * </p>
 */
@Stateless
public class AuthBean extends BaseBean {

    public static String JNDI_NAME = "java:global/api/AuthBean!xyz.shortbox.backend.ejb.AuthBean";

    /**
     * Cleans up all invalid {@code SessionEntity}.
     * <p>
     * The invalid {@link SessionEntity}. will be removed from database.
     * </p>
     */
    public void cleanupSessions() {
        List<SessionEntity> sessions = em.createNamedQuery("getAllSessions", SessionEntity.class).getResultList();

        if (sessions != null)
            sessions.forEach(session -> {
                try {
                    Timestamp now = new Timestamp(System.currentTimeMillis());
                    checkIfSessionIsActive(session, now);
                } catch (ForbiddenException e) {
                    //ignore ForbiddenException in cleanup process
                }
            });
    }

    /**
     * Creates a new {@code SessionEntity}.
     * <p>
     * The {@link SessionEntity} will be created for the corresponding {@link UserEntity}.
     * </p>
     * <p>
     * To log in the {@code UserEntity} has to be known, in {@code #UserState.ACTIVE}
     * and authenticated by the given password.
     * Also the {@code UserEntity} is not allowed to log in simultaneously from the same host.
     * </p>
     *
     * @param mail     the {@code UserEntity} mail.
     * @param password the {@code UserEntity} password.
     * @param host     the host to the {@code SessionEntitiy}.
     * @param keep     should the {@code SessionEntity} timeout or not?
     * @return the token to the created session.
     * @throws ForbiddenException    if the password is wrong.
     * @throws ConflictException     if the mail is already logged in or the {@code UserEntity}
     *                               is in the wrong state.
     * @throws UnauthorizedException if the mail is unknown.
     */
    public String login(String mail, String password, String host, boolean keep) throws ForbiddenException, ConflictException, UnauthorizedException {
        checkForExistingSession(host);

        UserEntity user = findUser(mail, password);
        checkIfUserIsActive(user);

        return createNewSession(user, keep, host);
    }

    /**
     * Removes the {@code SessionEntity} corresponding to the given token.
     *
     * @param token the token, that should be removed.
     * @throws NotFoundException if there is no {@link SessionEntity} corresponding to the given token.
     */
    public void logout(String token) throws NotFoundException {
        SessionEntity session = findSession(token);

        em.remove(session);
    }

    /**
     * Validates if the {@code SessionEntity} for the given token is still valid.
     * <p>
     * Also the {@link SessionEntity} will be refreshed.
     * </p>
     *
     * @param token the token, that should be checked and refreshed.
     * @return the corresponding {@link UserEntity} to the {@code SessionEntity}.
     * @throws NotFoundException  if there is no {@code SessionEntity} corresponding to the given token.
     * @throws ForbiddenException if the {@code SessionEntity} is not valid anymore.
     */
    public UserEntity validate(String token) throws NotFoundException, ForbiddenException {
        SessionEntity session = findSession(token);
        UserEntity user = findUser(session.getFkUser());

        Timestamp now = new Timestamp(System.currentTimeMillis());
        checkIfSessionIsActive(session, now);
        refreshSession(session, now);

        return user;
    }

    /**
     * Creates a new {@code SessionEntity}.
     * <p>
     * To do so a new unique session token will be generated.
     * Also the given {@link UserEntity} lastlogin will be updated (see {@link #updateUsersLastLogin(UserEntity)}).
     * </p>
     *
     * @param user the {@link UserEntity} the {@link SessionEntity} belongs to.
     * @param keep should the {@code SessionEntity} timeout or not?
     * @param host the host the {@code SessionEntity} belongs to.
     * @return the token to the created session.
     */
    private String createNewSession(UserEntity user, boolean keep, String host) {
        String token = keyGen.nextString();
        createSession(user, keep, host, token);

        updateUsersLastLogin(user);

        return token;
    }

    /**
     * Creates a new {@code SessionEntity}.
     *
     * @param user  the {@link UserEntity} the {@link SessionEntity} belongs to.
     * @param keep  should the {@code SessionEntity} timeout or not?
     * @param host  the host the {@code SessionEntity} belongs to.
     * @param token the unique session identifier.
     */
    private void createSession(UserEntity user, boolean keep, String host, String token) {
        SessionEntity session = new SessionEntity();
        session.setId(token);
        session.setFkUser(user.getId());
        session.setKeep((byte) (keep ? 1 : 0));
        session.setHost(host);

        em.persist(session);
    }

    /**
     * Updates the last login of an {@code UserEntity}.
     * <p>
     * The {@link UserEntity} lastlogin will be set to the {@code System.currenTimeMillis()}.
     * </p>
     *
     * @param user the {@code UserEntity} that should be updated.
     */
    private void updateUsersLastLogin(UserEntity user) {
        user.setLastlogin(new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Checks if given {@code UserEntity} is active.
     * <p>
     * The {@link UserEntity} has to be in state {#UserState.ACTIVE}.
     * </p>
     *
     * @param user the {@code UserEntity}, that should be checked.
     * @throws ForbiddenException if the {@code UserEntity} is not active.
     */
    private void checkIfUserIsActive(UserEntity user) throws ForbiddenException {
        if (!user.getState().equals(UserState.ACTIVE.name()))
            throw new ForbiddenException(Errors.USER_IS_IN_WRONG_STATE);
    }

    /**
     * Returns a {@code UserEntity}, corresponding to the given password and mail
     *
     * @param mail     the mail of the {@link UserEntity}.
     * @param password the password of the {@code UserEntity}.
     * @return the {@code UserEntity} corresponding to the given password and mail.
     * @throws UnauthorizedException if there is no {@code UserEntity} corresponding to the given
     *                               password and mail.
     */
    private UserEntity findUser(String mail, String password) throws UnauthorizedException {
        try {
            return em.createNamedQuery("findUserByMailAndPassword", UserEntity.class)
                    .setParameter("mail", mail)
                    .setParameter("password", password)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new UnauthorizedException(Errors.USER_NOT_FOUND);
        }
    }

    /**
     * Returns a {@code UserEntity}, corresponding to the given ID.
     *
     * @param id the ID of the {@link UserEntity}.
     * @return the {@code UserEntity} corresponding to the given ID.
     * @throws NotFoundException if there is no {@code UserEntity} corresponding to the given ID.
     */
    private UserEntity findUser(int id) throws NotFoundException {
        UserEntity user = em.find(UserEntity.class, id);

        if (user == null)
            throw new NotFoundException(Errors.USER_NOT_FOUND);

        return user;
    }

    /**
     * Checks weather there is already an active session for the given host.
     * <p>
     * To do so it checks for an already existing {@link SessionEntity} with the given host in the
     * database.
     * </p>
     *
     * @param host the host  that is not allowed to be duplicate.
     * @throws ConflictException if an active {@code SessionEntity} for the host already exists.
     */
    private void checkForExistingSession(String host) throws ConflictException {
        try {
            em.createNamedQuery("findSessionByHost", SessionEntity.class)
                    .setParameter("host", host)
                    .getSingleResult();

        } catch (Exception e) {
            //It's okay, these are no sessions for this host
            return;
        }

        throw new ConflictException(Errors.ALREADY_LOGGED_IN);
    }

    /**
     * Returns a {@code SessionEntity} corresponding to the given sessiontoken.
     *
     * @param token the sessiontoken, that should be searched for.
     * @return the corresponding {@code SessionEntity}.
     * @throws NotFoundException if there is no {@code SessionEntity} corresponding to the given token.
     */
    private SessionEntity findSession(String token) throws NotFoundException {
        SessionEntity session = em.find(SessionEntity.class, token);

        if (session == null)
            throw new NotFoundException(Errors.SESSION_NOT_FOUND);

        return session;
    }

    /**
     * Checks if a {@code SessionEntity} is still valid or not.
     * <p>
     * The lastaccess and the given {@link Timestamp} will be compared.
     * If the {@link SessionEntity} is invalid, the {@code SessionEntity} will be removed.
     * </p>
     *
     * @param session the {@code SessionEntity}, that should be checked.
     * @param now     the current {@code Timestamp}.
     * @throws ForbiddenException if the {@code SessionEntity} is not valid anymore.
     */
    private void checkIfSessionIsActive(SessionEntity session, Timestamp now) throws ForbiddenException {
        if (session.getKeep() == 0) {
            Calendar timeout = Calendar.getInstance();
            timeout.setTime(session.getLastaccess());
            timeout.add(Calendar.MINUTE, Configuration.SESSION_INVALID);

            if (timeout.getTime().before(now)) {
                em.remove(session);
                throw new ForbiddenException(Errors.SESSION_INVALID);
            }
        }
    }

    /**
     * Refreshes the given {@code SessionEntity}.
     * <p>
     * Sets the lastaccess {@link Timestamp} of a {@link SessionEntity} to the given {@link Timestamp}
     * to refresh the session.
     * </p>
     *
     * @param session the {@code SessionDTO}, that should be refreshed.
     * @param now     the current {@link Timestamp}.
     */
    private void refreshSession(SessionEntity session, Timestamp now) {
        session.setLastaccess(now);
    }
}
