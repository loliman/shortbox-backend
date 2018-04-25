package xyz.shortbox.backend.error;

/**
 * Contains all Shortbox internal errors.
 * <p>
 * A new {@link Error} SHOULD (not to say MUST) use a new error number, which should be the last error number plus one.
 * The error message SHOULD be self explainable, so that no additional documentation of the error should be needed.
 * </p>
 */
public interface Errors {

    Error UNKNOWN_ERROR = new Error(-1, "An unknown error occurred.");
    Error SESSION_INVALID = new Error(1, "Your session is invalid. Try to log in again.");
    Error SESSION_NOT_FOUND = new Error(2, "No session for token found.");
    Error USER_NOT_FOUND = new Error(3, "No user found.");
    Error ALREADY_LOGGED_IN = new Error(4, "An session four your IP already exists.");
    Error USER_IS_IN_WRONG_STATE = new Error(5, "This account is not in the right state. Is your registration completed?");
    Error NOT_ALLOWED = new Error(6, "Your are not allowed to call this method.");
    Error KEYS_DO_NOT_MATCH = new Error(7, "Provided and stored keys do not match.");
    Error INVALID_PARAMETERS = new Error(8, "Invalid request. Check provided parameters.");
    Error INVALID_MAIL = new Error(9, "Invalid mail. Check provided parameters.");
    Error USER_ALREADY_EXISTS = new Error(10, "User already exists.");
}
