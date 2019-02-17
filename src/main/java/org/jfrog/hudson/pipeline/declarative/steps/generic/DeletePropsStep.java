package org.jfrog.hudson.pipeline.declarative.steps.generic;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.SpecConfiguration;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.EditPropsExecutor;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.SpecUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.jfrog.build.extractor.clientConfiguration.util.EditPropertiesHelper.EditPropertiesActionType;

public class DeletePropsStep extends AbstractStepImpl {
    protected String serverId;
    protected String spec;
    private String props;
    private String specPath;
    boolean failNoOp;

    @DataBoundConstructor
    public DeletePropsStep(String serverId) {
        this.serverId = serverId;
    }

    @DataBoundSetter
    public void setSpec(String spec) {
        this.spec = spec;
    }

    @DataBoundSetter
    public void setSpecPath(String specPath) {
        this.specPath = specPath;
    }

    @DataBoundSetter
    public void setProps(String props) {
        this.props = props;
    }

    @DataBoundSetter
    public void setFailNoOp(boolean failNoOp) {
        this.failNoOp = failNoOp;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject(optional = true)
        private transient DeletePropsStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        protected ArtifactoryServer artifactoryServer;
        protected String spec;

        @Override
        protected Void run() throws Exception {
            // Set Artifactory server
            org.jfrog.hudson.pipeline.common.types.ArtifactoryServer pipelineServer = DeclarativePipelineUtils
                    .getArtifactoryServer(build, ws, getContext(), step.serverId);
            artifactoryServer = Utils.prepareArtifactoryServer(null, pipelineServer);

            // Set spec
            SpecConfiguration specConfiguration = new SpecConfiguration(step.spec, step.specPath);
            spec = SpecUtils.getSpecStringFromSpecConf(specConfiguration, env, ws, listener.getLogger());


            EditPropsExecutor editPropsExecutor = new EditPropsExecutor(artifactoryServer, listener, build, ws, spec,
                    EditPropertiesActionType.DELETE, step.props, step.failNoOp);
            editPropsExecutor.execute();
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DeletePropsStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtDeleteProps";
        }

        @Override
        public String getDisplayName() {
            return "Delete properties";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
