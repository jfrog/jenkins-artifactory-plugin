package org.jfrog.hudson.trigger;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.BuildableItem;
import hudson.model.Item;
import jenkins.branch.MultiBranchProject;
import org.jfrog.hudson.JFrogPlatformInstance;
import org.jfrog.hudson.ServerDetails;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collections;
import java.util.List;

/**
 * This class represents Artifactory trigger in UI and pipeline job types.
 *
 * @author yahavi
 */
public class ArtifactoryTrigger extends BaseTrigger<BuildableItem> {

    @DataBoundConstructor
    public ArtifactoryTrigger(String paths, String spec) throws ANTLRException {
        super(spec, paths, null);
    }

    @DataBoundSetter
    public void setDetails(ServerDetails details) {
        this.details = details;
    }

    @Override
    List<BuildableItem> getJobsToTrigger() {
        return Collections.singletonList(job);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends ArtifactoryTriggerDescriptor {

        @Override
        public boolean isApplicable(Item item) {
            if (!(item instanceof BuildableItem)) {
                return false;
            }
            try {
                return !(item instanceof MultiBranchProject);
            } catch (NoClassDefFoundError ignore) {
                // workflow-multibranch plugin is not installed
                return true;
            }
        }

        public List<JFrogPlatformInstance> getJfrogInstances() {
            return super.getJfrogInstances();
        }
    }
}