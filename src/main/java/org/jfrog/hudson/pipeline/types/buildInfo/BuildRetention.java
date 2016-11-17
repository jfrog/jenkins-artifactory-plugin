package org.jfrog.hudson.pipeline.types.buildInfo;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by Tamirh on 14/07/2016.
 */
public class BuildRetention implements Serializable {
    private boolean deleteBuildArtifacts;
    private int maxBuilds;
    private List<String> doNotDiscardBuilds;
    private int maxDays;


    public BuildRetention() {
        this.maxDays = -1;
        this.maxBuilds = -1;
    }

    public org.jfrog.build.api.BuildRetention build() {
        org.jfrog.build.api.BuildRetention buildRetention = new org.jfrog.build.api.BuildRetention();
        buildRetention.setCount(Math.max(maxBuilds, -1));

        if (maxDays > -1) {
            Calendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_MONTH, -1 * this.maxDays);
            buildRetention.setMinimumBuildDate(cal.getTime());
        }
        else {
            buildRetention.setMinimumBuildDate(null);
        }
        buildRetention.setBuildNumbersNotToBeDiscarded(this.getDoNotDiscardBuilds());
        buildRetention.setDeleteBuildArtifacts(this.deleteBuildArtifacts);
        return buildRetention;
    }

    @Whitelisted
    public void clear() {
        this.maxDays = -1;
        this.deleteBuildArtifacts = false;
        this.maxBuilds = -1;
        this.doNotDiscardBuilds = null;
    }

    @Whitelisted
    public int getMaxBuilds() {
        return this.maxBuilds;
    }

    @Whitelisted
    public void setMaxBuilds(int maxBuilds) {
        this.maxBuilds = maxBuilds;
    }

    @Whitelisted
    public int getMaxDays() {
        return this.maxDays;
    }

    @Whitelisted
    public void setMaxDays(int days) {
        this.maxDays = days;
    }

    @Whitelisted
    public void setDeleteBuildArtifacts(boolean deleteBuildArtifacts) {
        this.deleteBuildArtifacts = deleteBuildArtifacts;
    }

    @Whitelisted
    public boolean isDeleteBuildArtifacts() {
        return this.deleteBuildArtifacts;
    }

    @Whitelisted
    public void setDoNotDiscardBuilds(List<String> buildNumbersNotToBeDiscarded) {
        this.doNotDiscardBuilds = buildNumbersNotToBeDiscarded;
    }

    @Whitelisted
    public List<String> getDoNotDiscardBuilds() {
        return this.doNotDiscardBuilds;
    }
}
