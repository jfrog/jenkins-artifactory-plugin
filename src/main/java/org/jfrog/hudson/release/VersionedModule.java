package org.jfrog.hudson.release;

import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
public class VersionedModule implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String moduleName;
    private final String releaseVersion;
    private final String nextDevelopmentVersion;

    public VersionedModule(String moduleName, String releaseVersion, String nextDevelopmentVersion) {
        this.moduleName = moduleName;
        this.releaseVersion = releaseVersion;
        this.nextDevelopmentVersion = nextDevelopmentVersion;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public String getNextDevelopmentVersion() {
        return nextDevelopmentVersion;
    }
}
