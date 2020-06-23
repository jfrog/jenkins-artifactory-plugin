package org.jfrog.hudson.jfpipelines;

import hudson.Extension;
import hudson.model.StringParameterDefinition;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class JFrogPipelinesParameter extends StringParameterDefinition {
    public static final String PARAM_NAME = "JFROG_PIPELINES_INFO";
    private static final String DESCRIPTION = "This parameter is automatically injected by JFrog Pipelines integration";

    @DataBoundConstructor
    public JFrogPipelinesParameter(String name, String defaultValue, String description, boolean trim) {
        super(name, defaultValue, description, trim);
    }

    public JFrogPipelinesParameter(String defaultValue) {
        this(PARAM_NAME, defaultValue, DESCRIPTION, true);
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "JFrog Pipelines Information";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/artifactory/help/JFrogPipelines/parameter.html";
        }
    }
}
