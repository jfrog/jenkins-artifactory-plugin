package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.EditPropsExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.EditPropertiesHelper.EditPropertiesActionType;

public class EditPropsStep extends AbstractStepImpl {
    private ArtifactoryServer server;
    private String spec;
    private EditPropertiesActionType editType;
    private String props;
    private boolean failNoOp;

    @DataBoundConstructor
    public EditPropsStep(String spec, String props, boolean failNoOp, ArtifactoryServer server,
                         EditPropertiesActionType editType) {
        this.spec = spec;
        this.editType = editType;
        this.props = props;
        this.server = server;
        this.failNoOp = failNoOp;
    }

    public EditPropertiesActionType getEditType() {
        return editType;
    }

    public String getProps() {
        return props;
    }

    public String getSpec() {
        return spec;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public boolean getFailNoOp() {
        return failNoOp;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Boolean> {

        private transient EditPropsStep step;

        @Inject
        public Execution(EditPropsStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Boolean run() throws Exception {
            new EditPropsExecutor(Utils.prepareArtifactoryServer(null, step.getServer()),
                    this.listener, this.build, this.ws, Util.replaceMacro(step.getSpec(), env), step.getEditType(),
                    step.getProps(), step.getFailNoOp()).execute();
            return true;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(EditPropsStep.Execution.class);
        }

        @Override
        // The step is invoked by ArtifactoryServer by the step name
        public String getFunctionName() {
            return "artifactoryEditProps";
        }

        @Override
        public String getDisplayName() {
            return "Edit properties";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
