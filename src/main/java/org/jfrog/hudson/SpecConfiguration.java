package org.jfrog.hudson;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by diman on 13/10/2016.
 */
public class SpecConfiguration {

    private final String spec;
    private final String filePath;

    @DataBoundConstructor
    public SpecConfiguration(String spec, String filePath) {
        this.spec = spec;
        this.filePath = filePath;
    }

    public String getSpec() {
        return spec;
    }

    public String getFilePath() {
        return filePath;
    }
}
