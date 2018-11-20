package org.jfrog.hudson.pipeline.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.hudson.pipeline.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;

public class NpmPackageInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    @JsonProperty("name")
    private String name;
    @JsonProperty("version")
    private String version;
    private String scope;

    public NpmPackageInfo() {
    }

    public NpmPackageInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    private void removeVersionPrefixes() {
        version = StringUtils.removeStart(version, "v");
        version = StringUtils.removeStart(version, "=");
    }

    private void splitScopeFromName() {
        if (StringUtils.startsWith(name, "@") && StringUtils.contains(name, "/")) {
            String[] splitValues = StringUtils.split(name, "/");
            scope = splitValues[0];
            name = splitValues[1];
        }
    }

    public void readPackageInfo(final TaskListener listener, FilePath ws) throws IOException, InterruptedException {
        ws.act(new MasterToSlaveFileCallable<NpmPackageInfo>() {
            @Override
            public NpmPackageInfo invoke(File file, VirtualChannel channel) throws IOException {
                Path packageJsonPath = file.toPath().resolve("package.json");
                listener.getLogger().print("Reading info from package.json file: " + packageJsonPath);

                try (FileInputStream fis = new FileInputStream(packageJsonPath.toFile())) {
                    NpmPackageInfo packageInfo = Utils.mapper().readValue(fis, NpmPackageInfo.class);

                    setVersion(packageInfo.getVersion());
                    removeVersionPrefixes();

                    setName(packageInfo.getName());
                    splitScopeFromName();

                }
                return null;
            }

        });
    }
}
