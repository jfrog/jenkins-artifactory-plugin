package org.jfrog.hudson.pipeline;

import org.junit.Test;

import static org.jfrog.hudson.pipeline.common.Utils.escapeUnixArgument;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

    @Test
    public void escapeUnixArgumentTest() {
        validateEscapeUnixArgument("abcd", "abcd");
        validateEscapeUnixArgument("*abcd", "\\*abcd");
        validateEscapeUnixArgument("?abcd", "\\?abcd");
        validateEscapeUnixArgument("[abcd", "\\[abcd");
        validateEscapeUnixArgument("]abcd", "\\]abcd");
        validateEscapeUnixArgument("a*bcd", "a\\*bcd");
        validateEscapeUnixArgument("a?bcd", "a\\?bcd");
        validateEscapeUnixArgument("a[bcd", "a\\[bcd");
        validateEscapeUnixArgument("a]bcd", "a\\]bcd");
        validateEscapeUnixArgument("abcd*", "abcd\\*");
        validateEscapeUnixArgument("abcd?", "abcd\\?");
        validateEscapeUnixArgument("abcd[", "abcd\\[");
        validateEscapeUnixArgument("abcd]", "abcd\\]");
        validateEscapeUnixArgument("*", "\\*");
        validateEscapeUnixArgument("?", "\\?");
        validateEscapeUnixArgument("[", "\\[");
        validateEscapeUnixArgument("]", "\\]");
        validateEscapeUnixArgument("", "");
        validateEscapeUnixArgument("a*?[]b", "a\\*\\?\\[\\]b");
        validateEscapeUnixArgument("`", "\\`");
        validateEscapeUnixArgument("^", "\\^");
        validateEscapeUnixArgument("<", "\\<");
        validateEscapeUnixArgument(">", "\\>");
        validateEscapeUnixArgument("|", "\\|");
        validateEscapeUnixArgument(" ", "\\ ");
        validateEscapeUnixArgument(",", "\\,");
        validateEscapeUnixArgument(";", "\\;");
        validateEscapeUnixArgument("!", "\\!");
        validateEscapeUnixArgument("?", "\\?");
        validateEscapeUnixArgument("'", "\\'");
        validateEscapeUnixArgument("\"", "\\\"");
        validateEscapeUnixArgument("(", "\\(");
        validateEscapeUnixArgument(")", "\\)");
        validateEscapeUnixArgument("[", "\\[");
        validateEscapeUnixArgument("]", "\\]");
        validateEscapeUnixArgument("{", "\\{");
        validateEscapeUnixArgument("}", "\\}");
        validateEscapeUnixArgument("$", "\\$");
        validateEscapeUnixArgument("*", "\\*");
        validateEscapeUnixArgument("\\", "\\\\");
        validateEscapeUnixArgument("&", "\\&");
        validateEscapeUnixArgument("#", "\\#");
    }

    private void validateEscapeUnixArgument(String argToTest, String expected) {
        String res = escapeUnixArgument(argToTest);
        assertTrue(expected.equals(res));
    }
}
