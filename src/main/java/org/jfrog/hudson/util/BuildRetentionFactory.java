package org.jfrog.hudson.util;

import hudson.model.AbstractBuild;
import hudson.tasks.LogRotator;
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
    public static BuildRetention createBuildRetention(AbstractBuild build, boolean discardOldArtifacts) {
        BuildRetention buildRetention = new BuildRetention(discardOldArtifacts);
        LogRotator rotator = build.getProject().getLogRotator();
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
