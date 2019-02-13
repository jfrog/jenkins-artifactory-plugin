package org.jfrog.hudson.pipeline.common.types.buildInfo;

import com.google.common.collect.Lists;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by Tamirh on 14/07/2016.
 */
public class BuildRetention implements Serializable {
    public static final long serialVersionUID = 1L;

    private boolean deleteBuildArtifacts;
    private int maxBuilds;
    private List<String> doNotDiscardBuilds = Lists.newArrayList();
    private int maxDays;
    private boolean async;

    public BuildRetention() {
        clear();
    }

    public org.jfrog.build.api.BuildRetention createBuildRetention() {
        org.jfrog.build.api.BuildRetention buildRetention = new org.jfrog.build.api.BuildRetention();
        buildRetention.setCount(Math.max(maxBuilds, -1));

        if (maxDays > -1) {
            Calendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_MONTH, -1 * this.maxDays);
            buildRetention.setMinimumBuildDate(cal.getTime());
        } else {
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
        this.doNotDiscardBuilds = Lists.newArrayList();
        this.async = false;
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
        if (buildNumbersNotToBeDiscarded == null) {
            this.doNotDiscardBuilds = Lists.newArrayList();
            return;
        }
        this.doNotDiscardBuilds = buildNumbersNotToBeDiscarded;
    }

    @Whitelisted
    public List<String> getDoNotDiscardBuilds() {
        return this.doNotDiscardBuilds;
    }

    @Whitelisted
    public void setAsync(boolean async) {
        this.async = async;
    }

    @Whitelisted
    public boolean isAsync() {
        return this.async;
    }
}
