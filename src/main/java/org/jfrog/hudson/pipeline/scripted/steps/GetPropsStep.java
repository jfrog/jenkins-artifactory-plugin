package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.GetPropsExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

public class GetPropsStep extends AbstractStepImpl {
    static final String STEP_NAME = "artifactoryGetProps";
    private ArtifactoryServer server;
    private final String relativePath;
    private final List<String> propertyKeys;

    @DataBoundConstructor
    public GetPropsStep(StepContext context, ArtifactoryServer server, String relativePath, List<String> propertyKeys) {
        this.server = server;
        this.relativePath = relativePath;
        this.propertyKeys = propertyKeys;
    }

    public ArtifactoryServer getServer() { return server; }

    public String getRelativePath() { return relativePath; }

    public List<String> getPropertyKeys() { return propertyKeys; }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<ArrayListMultimap<String, String>> {
        protected static final long serialVersionUID = 1L;

        private transient GetPropsStep step;
        @Inject
        public Execution(GetPropsStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected ArrayListMultimap<String, String> runStep() throws Exception {
            GetPropsExecutor executor = new GetPropsExecutor(Utils.prepareArtifactoryServer(null, step.getServer()),
                    this.listener, this.build, this.ws, step.getRelativePath(), step.getPropertyKeys());
            executor.execute();
            return executor.getProperties();
        }

        @Override
        public org.jfrog.hudson.ArtifactoryServer getUsageReportServer() {
            return Utils.prepareArtifactoryServer(null, step.getServer());
        }

        @Override
        public String getUsageReportFeatureName() {
            return STEP_NAME;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GetPropsStep.Execution.class);
        }

        @Override
        // The step is invoked by ArtifactoryServer by the step name
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Get properties";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
