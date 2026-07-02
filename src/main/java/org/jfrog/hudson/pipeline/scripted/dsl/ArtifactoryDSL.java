package org.jfrog.hudson.pipeline.scripted.dsl;

import groovy.lang.Binding;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import javax.annotation.Nonnull;

/**
 * Created by Tamirh on 17/05/2016.
 */
@Extension
public class ArtifactoryDSL extends GlobalVariable {
    @Nonnull
    @Override
    public String getName() {
        return "Artifactory";
    }

    @Nonnull
    @Override
    public Object getValue(@Nonnull CpsScript cpsScript) throws Exception {
        Binding binding = cpsScript.getBinding();
        Object artifactory;
        if (binding.hasVariable(getName())) {
            artifactory = binding.getVariable(getName());
        } else {
            artifactory = new ArtifactoryPipelineGlobal(cpsScript);
            binding.setVariable(getName(), artifactory);
        }
        return artifactory;
    }
}
