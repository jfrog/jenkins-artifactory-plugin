package org.jfrog.hudson.pipeline.scripted.dsl;

import groovy.lang.Binding;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import javax.annotation.Nonnull;

@Extension
public class JFrogDSL extends GlobalVariable {
    @Nonnull
    @Override
    public String getName() {
        return "JFrog";
    }

    @Nonnull
    @Override
    public Object getValue(@Nonnull CpsScript cpsScript) throws Exception {
        Binding binding = cpsScript.getBinding();
        Object jfrog;
        if (binding.hasVariable(getName())) {
            jfrog = binding.getVariable(getName());
        } else {
            jfrog = new JFrogPipelineGlobal(cpsScript);
            binding.setVariable(getName(), jfrog);
        }
        return jfrog;
    }
}
