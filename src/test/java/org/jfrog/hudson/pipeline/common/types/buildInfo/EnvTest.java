package org.jfrog.hudson.pipeline.common.types.buildInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

/**
 * This class test the functionality of Env.
 *
 * @author yahavi
 * @see Env
 */
public class EnvTest {
    private EnvFilter envFilter = new EnvFilter();
    private Map<String, String> localEnv;

    @Before
    public void setUp() {
        localEnv = new HashMap<String, String>() {{
            putAll(System.getenv());
            put("password", "password");
            put("secret1", "secret");
            put("tOkEn", "token");
            put("psww", "psw");
            put("1key", "key");

            put("vision", "vision");
            put("wanda", "wanda");
        }};
    }

    @BeforeEach
    public void resetFilters() {
        envFilter.reset();
    }

    @Test
    public void defaultIncludeTest() {
        Env env = createFilteredEnv(envFilter);

        // 'wanda' and 'vision' should be included by default
        assertIncluded(env, "wanda");
        assertIncluded(env, "vision");
    }

    @Test
    public void defaultExcludeTest() {
        Env env = createFilteredEnv(envFilter);

        // These values should be excluded by default
        assertNotIncluded(env, "password");
        assertNotIncluded(env, "secret1");
        assertNotIncluded(env, "tOkEn");
        assertNotIncluded(env, "psww");
        assertNotIncluded(env, "1key");
    }

    @Test
    public void includeTest() {
        envFilter.addInclude("wanda");
        Env env = createFilteredEnv(envFilter);

        // Only 'wanda' should be included
        assertIncluded(env, "wanda");
        assertNotIncluded(env, "vision");
        assertNotIncluded(env, "notIncludedEnv2");
    }

    @Test
    public void excludeTest() {
        envFilter.addExclude("vision");
        Env env = createFilteredEnv(envFilter);

        // These values should be excluded by default
        assertNotIncluded(env, "password");
        assertNotIncluded(env, "secret1");
        assertNotIncluded(env, "tOkEn");
        assertNotIncluded(env, "1key");
        assertNotIncluded(env, "psww");

        // 'vision' should be excluded, but not 'wanda'
        assertNotIncluded(env, "vision");
        assertIncluded(env, "wanda");
    }

    /**
     * Assert Env includes the key.
     *
     * @param env - The environment variables to check
     * @param key - The key to check
     */
    private void assertIncluded(Env env, String key) {
        Assert.assertTrue(env.getEnvVars().containsKey(key));
        Assert.assertTrue(env.getSysVars().containsKey(key));
        Assert.assertTrue(env.getVars().containsKey(key));
    }

    /**
     * Assert Env not includes the key.
     *
     * @param env - The environment variables to check
     * @param key - The key to check
     */
    private void assertNotIncluded(Env env, String key) {
        Assert.assertFalse(env.getEnvVars().containsKey(key));
        Assert.assertFalse(env.getSysVars().containsKey(key));
        Assert.assertFalse(env.getVars().containsKey(key));
    }

    /**
     * Create a new Env with the input env filter.
     *
     * @param envFilter - The env filter
     * @return a new Env with the input env filter.
     */
    private Env createFilteredEnv(EnvFilter envFilter) {
        return new Env() {{
            setFilter(envFilter);
            setEnvVars(localEnv);
            setSysVars(localEnv);
            filter();
        }};
    }
}
