package org.jfrog.hudson.util;

import hudson.matrix.MatrixRun;
import hudson.model.Run;
import hudson.tasks.LogRotator;
import jenkins.model.BuildDiscarder;
import org.jfrog.build.api.BuildRetention;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Tomer Cohen
 */
public class BuildRetentionFactory {

    /**
     * Create a Build retention object out of the build
     *
     * @param build               The build to create the build retention out of
     * @param discardOldArtifacts Flag whether to discard artifacts of those builds that are to be discarded.
     * @return a new Build retention
     */
    public static BuildRetention createBuildRetention(Run build, boolean discardOldArtifacts) {
        BuildRetention buildRetention = new BuildRetention(discardOldArtifacts);
        LogRotator rotator = null;
        BuildDiscarder buildDiscarder;
        if (build instanceof MatrixRun) {
            // Get the BuildDiscarder of the MatrixProject.
            buildDiscarder = ((MatrixRun) build).getProject().getParent().getBuildDiscarder();
        } else {
            buildDiscarder = build.getParent().getBuildDiscarder();
        }

        if (buildDiscarder != null && buildDiscarder instanceof LogRotator) {
            rotator = (LogRotator)  buildDiscarder;
        }
        if (rotator == null) {
            return buildRetention;
        }
        if (rotator.getNumToKeep() > -1) {
            buildRetention.setCount(rotator.getNumToKeep());
        }
        if (rotator.getDaysToKeep() > -1) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -rotator.getDaysToKeep());
            buildRetention.setMinimumBuildDate(new Date(calendar.getTimeInMillis()));
        }
        List<String> notToBeDeleted = ExtractorUtils.getBuildNumbersNotToBeDeleted(build);
        buildRetention.setBuildNumbersNotToBeDiscarded(notToBeDeleted);
        return buildRetention;
    }
}
