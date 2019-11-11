package org.jfrog.hudson.pipeline.common.types.buildInfo;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.api.IssueTracker;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class Issues implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient CpsScript cpsScript;
    private String buildName;
    private String trackerName;
    private boolean aggregateBuildIssues;
    private String aggregationBuildStatus;
    private Set<Issue> affectedIssues;

    public Issues() {
    }

    /**
     * If one of the objects did not collect issues (empty tracker or affected issues) - keep the other.
     * If both collected and the tracker names match, append affected issues.
     * Otherwise, keep this original object.
     */
    public void append(Issues issuesToAppend) {
        if (issuesToAppend == null || issuesToAppend.trackerName == null || issuesToAppend.affectedIssues == null) {
            return;
        }
        if (this.trackerName == null || this.affectedIssues == null) {
            this.trackerName = issuesToAppend.trackerName;
            this.aggregateBuildIssues = issuesToAppend.aggregateBuildIssues;
            this.aggregationBuildStatus = issuesToAppend.aggregationBuildStatus;
            this.affectedIssues = issuesToAppend.affectedIssues;
            return;
        }
        if (issuesToAppend.trackerName.equals(this.trackerName)) {
            this.appendAffectedIssues(issuesToAppend.affectedIssues);
        }
    }

    public void convertAndAppend(org.jfrog.build.api.Issues issuesToAppend) {
        append(toPipelineIssues(issuesToAppend));
    }

    /**
     * Converts Issues of type org.jfrog.build.api.Issues to pipeline Issues
     */
    static Issues toPipelineIssues(org.jfrog.build.api.Issues issuesToConvert) {
        if (issuesToConvert == null) {
            return null;
        }

        Issues newIssues = new Issues();
        if (issuesToConvert.getTracker() != null) {
            newIssues.setTrackerName(issuesToConvert.getTracker().getName());
        }
        newIssues.setAggregateBuildIssues(issuesToConvert.isAggregateBuildIssues());
        newIssues.setAggregationBuildStatus(issuesToConvert.getAggregationBuildStatus());

        Set<org.jfrog.build.api.Issue> affectedIssuesToConvert = issuesToConvert.getAffectedIssues();
        if (affectedIssuesToConvert == null) {
            return newIssues;
        }
        Set<Issues.Issue> convertedAffectedIssues = new HashSet<>();
        for (org.jfrog.build.api.Issue issueToConvert : affectedIssuesToConvert) {
            convertedAffectedIssues.add(new Issues.Issue(issueToConvert.getKey(), issueToConvert.getUrl(), issueToConvert.getSummary()));
        }
        newIssues.setAffectedIssues(convertedAffectedIssues);
        return newIssues;
    }

    /**
     * Converts pipeline Issues to type org.jfrog.build.api.Issues
     */
    org.jfrog.build.api.Issues convertFromPipelineIssues(Issues this) {
        IssueTracker tracker = new IssueTracker(this.getTrackerName());

        Set<Issue> affectedIssuesToConvert = this.getAffectedIssues();
        if (affectedIssuesToConvert == null) {
            return new org.jfrog.build.api.Issues(tracker,
                    this.isAggregateBuildIssues(), this.getAggregationBuildStatus(), null);
        }

        Set<org.jfrog.build.api.Issue> convertedAffectedIssues = new HashSet<>();
        for (Issue issueToConvert : affectedIssuesToConvert) {
            convertedAffectedIssues.add(new org.jfrog.build.api.Issue(issueToConvert.getKey(), issueToConvert.getUrl(), issueToConvert.getSummary()));
        }

        return new org.jfrog.build.api.Issues(tracker,
                this.isAggregateBuildIssues(), this.getAggregationBuildStatus(), convertedAffectedIssues);
    }

    private void appendAffectedIssues(Set<Issue> affectedIssuesToAppend) {
        if (affectedIssuesToAppend == null) {
            return;
        }
        if (this.affectedIssues == null) {
            this.affectedIssues = affectedIssuesToAppend;
            return;
        }
        this.affectedIssues.addAll(affectedIssuesToAppend);
    }

    /**
     * Used to invoke the step in a scripted pipeline
     * */
    @Whitelisted
    public void collect(ArtifactoryServer server, String config) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("issues", this);
        stepVariables.put("server", server);
        stepVariables.put("config", config);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("collectIssues", stepVariables);
    }

    public String getTrackerName() {
        return trackerName;
    }

    public void setTrackerName(String trackerName) {
        this.trackerName = trackerName;
    }

    public boolean isAggregateBuildIssues() {
        return aggregateBuildIssues;
    }

    public void setAggregateBuildIssues(boolean aggregateBuildIssues) {
        this.aggregateBuildIssues = aggregateBuildIssues;
    }

    public String getAggregationBuildStatus() {
        return aggregationBuildStatus;
    }

    public void setAggregationBuildStatus(String aggregationBuildStatus) {
        this.aggregationBuildStatus = aggregationBuildStatus;
    }

    @Whitelisted
    public Set<Issue> getAffectedIssues() {
        return affectedIssues;
    }

    public void setAffectedIssues(Set<Issue> affectedIssues) {
        this.affectedIssues = affectedIssues;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public CpsScript getCpsScript() {
        return cpsScript;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    public static class Issue implements Serializable {
        private static final long serialVersionUID = 1L;

        private String key;
        private String url;
        private String summary;
        private boolean aggregated;

        public Issue() {
        }

        public Issue(String key, String url, String summary) {
            this.key = key;
            this.url = url;
            this.summary = summary;
            this.aggregated = false;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public boolean isAggregated() {
            return aggregated;
        }

        public void setAggregated(boolean aggregated) {
            this.aggregated = aggregated;
        }
    }
}
