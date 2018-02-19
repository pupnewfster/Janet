package gg.galaxygaming.janet;

/**
 * A generic Utilities class for {@link gg.galaxygaming.janet.Janet}.
 */
public class Utils {
    /**
     * Checks if the given string is a valid integer.
     * @param input The string to check.
     * @return True if the input is a valid integer, false otherwise.
     */
    public static boolean legalInt(String input) {
        try {
            Integer.parseInt(input);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}