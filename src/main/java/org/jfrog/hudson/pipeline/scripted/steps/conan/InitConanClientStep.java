package org.jfrog.hudson.pipeline.scripted.steps.conan;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ConanClient;
import org.jfrog.hudson.util.ExtractorUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.util.Calendar;

public class InitConanClientStep extends AbstractStepImpl {

    private ConanClient client;

    @DataBoundConstructor
    public InitConanClientStep(ConanClient client) {
        this.client = client;
    }

    public ConanClient getClient() {
        return client;
    }

    public static class Execution extends AbstractSynchronousStepExecution<Boolean> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject(optional = true)
        private transient InitConanClientStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected Boolean run() throws Exception {
            ConanClient conanClient = getConanClient();
            EnvVars extendedEnv = new EnvVars(env);
            extendedEnv.put(Utils.CONAN_USER_HOME, conanClient.getUserPath());
            ArgumentListBuilder args = new ArgumentListBuilder();
            String logFilePath = conanClient.getLogFilePath();
            args.addTokenized("conan config set");
            // We need to add quotation marks before we save the log file path
            args.add("log.trace_file=\"" + StringUtils.trim(logFilePath) + "\"");
            Utils.exeConan(args, ws, launcher, listener, build, extendedEnv);
            return true;
        }

        private ConanClient getConanClient() throws Exception {
            ConanClient conanClient = step.getClient();
            FilePath conanHomeDirectory;
            if (StringUtils.isEmpty(conanClient.getUserPath())) {
                conanHomeDirectory = env.containsKey(Utils.CONAN_USER_HOME) ? new FilePath(new File(env.get(Utils.CONAN_USER_HOME))) : createConanTempHome();
            } else {
                conanHomeDirectory = new FilePath(launcher.getChannel(), conanClient.getUserPath());
                if (!conanHomeDirectory.exists()) {
                    conanHomeDirectory.mkdirs();
                }
            }

            conanClient.setUserPath(conanHomeDirectory.getRemote());
            conanHomeDirectory.child(ConanClient.CONAN_LOG_FILE).touch(Calendar.getInstance().getTimeInMillis());
            return conanClient;
        }

        private FilePath createConanTempHome() throws Exception {
            // Create the @tmp directory
            FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);

            // Create the conan directory
            return tempDir.createTempDir("conan", "");
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(InitConanClientStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "initConanClient";
        }

        @Override
        public String getDisplayName() {
            return "Create Conan Client";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}