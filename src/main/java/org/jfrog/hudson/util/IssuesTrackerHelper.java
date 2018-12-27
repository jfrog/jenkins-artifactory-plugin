package org.jfrog.hudson.util;

import com.google.common.collect.Sets;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.jira.JiraSite;
import hudson.plugins.jira.model.JiraIssue;
import hudson.plugins.jira.selector.AbstractIssueSelector;
import hudson.plugins.jira.selector.DefaultIssueSelector;
import org.jfrog.build.api.Issue;
import org.jfrog.build.api.IssueTracker;
import org.jfrog.build.api.Issues;
import org.jfrog.build.api.IssuesTrackerFields;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.util.IssuesTrackerUtils;
import org.jfrog.hudson.util.plugins.PluginsUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class IssuesTrackerHelper {

    private String issueTrackerVersion;
    private boolean aggregateBuildIssues;
    private String aggregationBuildStatus;
    private String affectedIssues;
    private String deploymentProperties;

    public IssuesTrackerHelper(Run build, TaskListener listener, boolean aggregateBuildIssues,
                               String aggregationBuildStatus) {
        this.aggregateBuildIssues = aggregateBuildIssues;
        this.aggregationBuildStatus = aggregationBuildStatus;
        JiraSite site = JiraSite.get(build.getParent());
        if (site == null) {
                return;
        }

        try {
            issueTrackerVersion = getJiraVersion(site);
            StringBuilder affectedIssuesBuilder = new StringBuilder();
            StringBuilder deploymentPropertiesBuilder = new StringBuilder();
            Set<String> issueIds = Sets.newHashSet(manuallyCollectIssues(build, site, listener));
            for (String issueId : issueIds) {
                if (!site.existsIssue(issueId)) {
                    continue;
                }

                if (affectedIssuesBuilder.length() > 0) {
                    affectedIssuesBuilder.append(",");
                    deploymentPropertiesBuilder.append(",");
                }

                URL url = site.getUrl(issueId);
                JiraIssue issue = site.getIssue(issueId);
                affectedIssuesBuilder.append(issueId).append(">>").append(url.toString()).append(">>").append(
                        issue.getSummary());
                deploymentPropertiesBuilder.append(issueId);
            }
            affectedIssues = affectedIssuesBuilder.toString();
            deploymentProperties = deploymentPropertiesBuilder.toString();
        } catch (Exception e) {
            listener.getLogger()
                    .print("[Warning] Error while trying to collect issue tracker and change information: " +
                            e.getMessage());
        }
    }

    private String getJiraVersion(JiraSite site) {
        return PluginsUtils.getJiraVersion(site.url);
    }


    private Set<String> manuallyCollectIssues(Run build, JiraSite site, TaskListener listener)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        AbstractIssueSelector issueSelector = new DefaultIssueSelector();
        return issueSelector.findIssueIds(build, site, listener);
    }

    /**
     * Applying issues tracker info to a client configuration (used by the extractors)
     */
    public void setIssueTrackerInfo(ArtifactoryClientConfiguration configuration) {
        configuration.info.issues.setIssueTrackerName("JIRA");
        configuration.info.issues.setIssueTrackerVersion(issueTrackerVersion);
        configuration.info.issues.setAggregateBuildIssues(aggregateBuildIssues);
        if (aggregateBuildIssues) {
            configuration.info.issues.setAggregationBuildStatus(aggregationBuildStatus);
        } else {
            configuration.info.issues.setAggregationBuildStatus("");
        }
        configuration.info.issues.setAffectedIssues(affectedIssues);
        configuration.publisher.addMatrixParam(IssuesTrackerFields.AFFECTED_ISSUES, deploymentProperties);
    }

    /**
     * Apply issues tracker info to a build info builder (used by generic tasks and maven2 which doesn't use the extractor
     */
    public void setIssueTrackerInfo(BuildInfoBuilder builder) {
        Issues issues = new Issues();
        issues.setAggregateBuildIssues(aggregateBuildIssues);
        issues.setAggregationBuildStatus(aggregationBuildStatus);
        issues.setTracker(new IssueTracker("JIRA", issueTrackerVersion));
        Set<Issue> affectedIssuesSet = IssuesTrackerUtils.getAffectedIssuesSet(affectedIssues);
        if (!affectedIssuesSet.isEmpty()) {
            issues.setAffectedIssues(affectedIssuesSet);
        }
        builder.issues(issues);
    }
}
