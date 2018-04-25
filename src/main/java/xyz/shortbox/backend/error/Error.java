package xyz.shortbox.backend.error;

/**
 * Represents an Shortbox internal Error.
 * <p>
 * Both {@code code} and {@code message} must be set.
 * </p>
 */
public class Error {
    private int code;
    private String message;

    public Error(int code, String message) {
        if (message == null)
            throw new IllegalArgumentException();

        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}
