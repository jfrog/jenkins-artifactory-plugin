package org.jfrog.hudson.util;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * Holds include and exclude patterns
 *
 * @author Noam Y. Tenne
 */
public class IncludesExcludes implements Serializable {
    private final String includePatterns;
    private final String excludePatterns;

    @DataBoundConstructor
    public IncludesExcludes(String includePatterns, String excludePatterns) {
        this.includePatterns = includePatterns;
        this.excludePatterns = excludePatterns;
    }

    public String getIncludePatterns() {
        return includePatterns;
    }

    public String getExcludePatterns() {
        return excludePatterns;
    }
}
