package xyz.shortbox.backend.config;

/**
 * General config interface for shortbox
 */
public interface Configuration {
    /**
     * Enables/Disables the overall testmode for shortbox.
     */
    boolean TEST_MODE = true;

    /**
     * Enables/Disables the mail process for shortbox.
     * Does not work if {@code #Configuration.TEST_MODE} is false.
     */
    boolean DISABLE_MAIL = false;

    //General mail configuration
    String MAIL_PASSWORD = "lkwbnwtbwbpihjke";
    String MAIL_USER = "christian.riese";
    String MAIL_HOST = "smtp.gmail.com";
    Integer MAIL_PORT = 587;

    /**
     * How long should a session be valid?
     */
    Integer SESSION_INVALID = 1;
}
