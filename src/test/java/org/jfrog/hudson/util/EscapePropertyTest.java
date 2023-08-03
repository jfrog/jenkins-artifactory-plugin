package org.jfrog.hudson.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.jfrog.hudson.util.ExtractorUtils.escapeProperty;

@RunWith(Parameterized.class)
public class EscapePropertyTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"test", "test"},
                {"test | test", "test \\| test"},
                {"test \\| test", "test \\| test"},
                {"test \\ test", "test \\ test"},
                {"test \\\\ test", "test \\\\ test"},
                {"id | tests = test1,test2;", "id \\| tests \\= test1\\,test2\\;"},
                {"id \\| tests = test1,test2\\;", "id \\| tests \\= test1\\,test2\\;"},
        });
    }

    private final String property;
    private final String expectedString;

    public EscapePropertyTest(String property, String expectedString) {
        this.property = property;
        this.expectedString = expectedString;
    }

    @Test
    public void testEscapeProperty() {
        Assert.assertEquals(expectedString, escapeProperty(property));
    }
}
