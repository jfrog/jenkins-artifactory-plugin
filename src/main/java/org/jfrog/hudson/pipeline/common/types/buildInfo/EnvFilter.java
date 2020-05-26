package org.jfrog.hudson.pipeline.common.types.buildInfo;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.hudson.pipeline.common.types.Filter;

/**
 * Created by romang on 6/22/16.
 */
public class EnvFilter extends Filter {

    private final String DEFAULT_EXCLUDE_PATTERN = "*password*,*psw*,*secret*,*key*,*token*";

    public EnvFilter() {
        reset();
    }

    @Whitelisted
    public EnvFilter reset() {
        patternFilter = new IncludeExcludePatterns("", DEFAULT_EXCLUDE_PATTERN);
        return this;
    }
}
