package org.jfrog.hudson.util;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.slaves.SlaveComputer;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Noam Y. Tenne
 */
public class PluginDependencyHelper {

    public static FilePath getActualDependencyDirectory(AbstractBuild build, File localDependencyFile)
            throws IOException, InterruptedException {

        File localDependencyDir = localDependencyFile.getParentFile();
//        if (!(Computer.currentComputer() instanceof SlaveComputer)) {
//            return new FilePath(localDependencyDir);
//        }

        String pluginVersion = Hudson.getInstance().getPluginManager().getPlugin("artifactory").getVersion();
        if (pluginVersion.contains(" ")) {
            //Trim the plugin version in case we're working on a snapshot version (contains illegal chars)
            pluginVersion = StringUtils.split(pluginVersion, " ")[0];
        }

        FilePath remoteDependencyDir = new FilePath(build.getBuiltOn().getRootPath(),"cache/artifactory-plugin/" + pluginVersion);

        if (!remoteDependencyDir.exists()) {
            remoteDependencyDir.mkdirs();
        }

        //Check if the dependencies have already been transferred successfully
        FilePath remoteDependencyMark = new FilePath(remoteDependencyDir, "ok");
        if (!remoteDependencyMark.exists()) {

            File[] localDependencies = localDependencyDir.listFiles();
            for (File localDependency : localDependencies) {
                if (localDependency.getName().equals("classes.jar"))
                    // skip classes in this plugin source tree.
                    // TODO: for a proper long term fix, see my comment in JENKINS-18401
                    continue;
                FilePath remoteDependencyFilePath = new FilePath(remoteDependencyDir, localDependency.getName());
                if (!remoteDependencyFilePath.exists()) {
                    FilePath localDependencyFilePath = new FilePath(localDependency);
                    localDependencyFilePath.copyTo(remoteDependencyFilePath);
                }
            }

            //Mark that all the dependencies have been transferred successfully for future references
            remoteDependencyMark.touch(System.currentTimeMillis());
        }

        return remoteDependencyDir;
    }
}
