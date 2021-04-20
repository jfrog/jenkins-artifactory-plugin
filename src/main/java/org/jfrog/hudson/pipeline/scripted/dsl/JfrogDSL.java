package org.jfrog.hudson.pipeline.scripted.dsl;

import groovy.lang.Binding;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import javax.annotation.Nonnull;

@Extension
public class JfrogDSL extends GlobalVariable {
    @Nonnull
    @Override
    public String getName() {
        return "Jfrog";
    }

    @Nonnull
    @Override
    public Object getValue(@Nonnull CpsScript cpsScript) throws Exception {
        Binding binding = cpsScript.getBinding();
        Object jfrog;
        if (binding.hasVariable(getName())) {
            jfrog = binding.getVariable(getName());
        } else {
            jfrog = new JfrogPipelineGlobal(cpsScript);
            binding.setVariable(getName(), jfrog);
        }
        return jfrog;
    }
}
