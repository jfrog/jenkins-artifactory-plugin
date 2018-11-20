package org.jfrog.hudson.pipeline.types;

import org.jfrog.build.api.Dependency;
import org.jfrog.build.extractor.npm.types.NpmScope;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

public class NpmInstall implements Serializable {
    private static final long serialVersionUID = 1L;
    private String executablePath;
    private String workingDirectory;
    private String registry;
    private Properties npmAuth;
    private boolean collectBuildInfo;
    private List<Dependency> dependencies;
    private NpmScope scope;
    private NpmPackageInfo npmPackageInfo;

    public String getExecutablePath() {
        return executablePath;
    }

    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public Properties getNpmAuth() {
        return npmAuth;
    }

    public void setNpmAuth(Properties npmAuth) {
        this.npmAuth = npmAuth;
    }

    public boolean isCollectBuildInfo() {
        return collectBuildInfo;
    }

    public void setCollectBuildInfo(boolean collectBuildInfo) {
        this.collectBuildInfo = collectBuildInfo;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public NpmScope getScope() {
        return scope;
    }

    public void setScope(NpmScope scope) {
        this.scope = scope;
    }

    public NpmPackageInfo getNpmPackageInfo() {
        return npmPackageInfo;
    }

    public void setNpmPackageInfo(NpmPackageInfo npmPackageInfo) {
        this.npmPackageInfo = npmPackageInfo;
    }
}
