package org.jfrog.hudson.trigger;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.model.Job;
import jenkins.branch.MultiBranchProject;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty;
import org.jfrog.hudson.JFrogPlatformInstance;
import org.jfrog.hudson.ServerDetails;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents Artifactory trigger in multibranch job types.
 *
 * @author yahavi
 */
public class ArtifactoryMultibranchTrigger extends BaseTrigger<MultiBranchProject<?, ?>> {

    @DataBoundConstructor
    public ArtifactoryMultibranchTrigger(ServerDetails details, String paths, String spec, String branches) throws ANTLRException {
        super(spec, paths, branches);
        this.details = details;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    List<BuildableItem> getJobsToTrigger() {
        return getJobsToTrigger(job.getAllJobs());
    }

    @SuppressWarnings("rawtypes")
    List<BuildableItem> getJobsToTrigger(Collection<? extends Job> jobs) {
        List<BuildableItem> jobsToTrigger = new ArrayList<>();
        // Collect all branches requested to trigger. If no branches requested, trigger all jobs.
        Set<String> inputBranches = Arrays.stream(StringUtils.split(this.branches, ";"))
                .map(String::trim)
                .collect(Collectors.toSet());
        boolean triggerAll = inputBranches.isEmpty();
        for (Job<?, ?> branchJob : jobs) {
            // Iterate over all jobs in the multibranch job. If a branch is missing, it will be logger later.
            if (triggerAll || inputBranches.removeIf(inputBranch -> inputBranch.equals(getBranchName(branchJob)))) {
                jobsToTrigger.add((BuildableItem) branchJob);
            }
        }
        if (!inputBranches.isEmpty()) {
            logger.warning("The following branches does not exist in multibranch pipeline '" +
                    Objects.requireNonNull(job).getName() + "': " + String.join(", ", inputBranches));
        }
        return jobsToTrigger;
    }

    /**
     * Get the name of the Git branch of the branch job.
     *
     * @param branchJob - The branch to query
     * @return the branch name
     */
    String getBranchName(Job<?, ?> branchJob) {
        BranchJobProperty branchJobProperty = branchJob.getProperty(BranchJobProperty.class);
        return branchJobProperty == null ? "" : branchJobProperty.getBranch().getHead().getName();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends ArtifactoryTriggerDescriptor {

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof MultiBranchProject;
        }

        public List<JFrogPlatformInstance> getJfrogInstances() {
            return super.getJfrogInstances();
        }
    }
}