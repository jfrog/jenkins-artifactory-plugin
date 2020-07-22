package org.jfrog.hudson.pipeline.common.types;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yahavi
 */
public class GradlePublications implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> publications = new ArrayList<>();

    public void setPublications(List<String> publications) {
        this.publications = publications;
    }

    public List<String> getPublications() {
        return this.publications;
    }

    @Whitelisted
    public GradlePublications add(String publication) {
        this.publications.add(publication);
        return this;
    }
}
