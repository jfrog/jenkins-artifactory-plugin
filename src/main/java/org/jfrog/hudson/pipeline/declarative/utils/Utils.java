package org.jfrog.hudson.pipeline.declarative.utils;

import hudson.FilePath;

public class Utils {

    /**
     * Create pipeline build data in @tmp directory.
     * The build data can be one of 'artifactoryMaven', 'mavenDeploy', 'mavenResolve', 'buildInfo' and other declarative pipeline steps.
     * @param buildNumber - The build number
     * @param stepName - The step name - On of 'artifactoryMaven', 'mavenDeploy', 'mavenResolve', 'buildInfo' and other declarative pipeline steps.
     * @param stepId - The step id from the user.
     * @param content - The content of the data file.
     * @return (String) - The name of the file
     * @throws Exception - In case of missing permissions.
     */
    public static void createTempBuildDataFile(FilePath ws, final String buildNumber, final String stepName, final String stepId, final String content) throws Exception {
        FilePath tempDirPath = new FilePath(ws.getParent(), ws.getName() + "@tmp");
        tempDirPath.act(new CreateBuildFileCallable(buildNumber, stepName, stepId, content));
    }
}
