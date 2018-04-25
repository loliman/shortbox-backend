package xyz.shortbox.backend.rest.util;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;

/**
 * Producer class for unique tokens.
 * <p>
 * Idea taken from https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string/41156#41156
 * </p>
 */
public class UniqueTokenGenerator {

    private static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String lower = upper.toLowerCase(Locale.ROOT);
    private static final String digits = "0123456789";
    private static final String alphanum = upper + lower + digits;

    private final Random random;
    private final char[] symbols;
    private final char[] buf;

    /**
     * Creates a new instance of {@code UniqueTokenGenerator}.
     *
     * @param length the length of generated tokens.
     */
    public UniqueTokenGenerator(int length) {
        this.random = new SecureRandom();
        this.symbols = alphanum.toCharArray();
        this.buf = new char[length];
    }

    /**
     * Produces a new unique token.
     * <p>
     * The returned token is based on upper- and lowercase characters and digits.
     * </p>
     *
     * @return the generated token.
     */
    public String nextString() {
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }
}