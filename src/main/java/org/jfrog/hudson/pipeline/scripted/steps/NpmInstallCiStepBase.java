package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.NpmInstallCiExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NpmBuild;

import java.io.IOException;

/**
 * Created by Bar Belity on 19/11/2020.
 */
abstract public class NpmInstallCiStepBase extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private String javaArgs;
    private String path;
    private String args;
    private String module;
    private boolean isCiCommand;
    private NpmBuild npmBuild;

    protected NpmInstallCiStepBase(BuildInfo buildInfo, NpmBuild npmBuild, String javaArgs, String path, String args, String module, boolean isCiCommand) {
        this.buildInfo = buildInfo;
        this.npmBuild = npmBuild;
        this.javaArgs = javaArgs;
        this.path = path;
        this.args = args;
        this.module = module;
        this.isCiCommand = isCiCommand;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient NpmInstallCiStepBase step;

        @Inject
        public Execution(NpmInstallCiStepBase step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            Utils.addNpmToPath(ws, listener, env, launcher, step.npmBuild.getTool());
            NpmInstallCiExecutor npmInstallCiExecutor = new NpmInstallCiExecutor(step.buildInfo, launcher, step.npmBuild, step.javaArgs, step.args, ws, step.path, step.module, env, listener, build, step.isCiCommand);
            npmInstallCiExecutor.execute();
            return npmInstallCiExecutor.getBuildInfo();
        }
    }
}
