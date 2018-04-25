package xyz.shortbox.backend.ejb.timer;

import xyz.shortbox.backend.config.Configuration;
import xyz.shortbox.backend.ejb.AuthBean;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Periodically cleans up all invalid {@code SessionEntity}.
 */
@Startup
@Singleton
public class SessionCleanupTimer extends TimerTask {

    /**
     * Schedule the timer.
     */
    @PostConstruct
    public void init() {
        Timer time = new Timer();
        SessionCleanupTimer st = new SessionCleanupTimer();
        time.schedule(st, 0, Configuration.SESSION_INVALID * 60 * 1000);
    }

    public void run() {
        try {
            AuthBean authBean = (AuthBean) new InitialContext().lookup(AuthBean.JNDI_NAME);
            authBean.cleanupSessions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
