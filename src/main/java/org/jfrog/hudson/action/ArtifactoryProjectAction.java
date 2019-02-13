/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson.action;

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.ProminentProjectAction;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.RepositoriesUtils;

/**
 * {@link hudson.model.ProminentProjectAction} that links to the latest build in Artifactory.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryProjectAction implements ProminentProjectAction {
    private final String urlSuffix;
    private final String artifactoryServerName;

    public ArtifactoryProjectAction(String artifactoryServerName, AbstractProject<?, ?> project) {
        urlSuffix = generateUrlSuffix(project);
        this.artifactoryServerName = artifactoryServerName;
    }

    public ArtifactoryProjectAction(String artifactoryServerName, String buildName) {
        urlSuffix = generateUrlSuffix(buildName);
        this.artifactoryServerName = artifactoryServerName;
    }

    public String getIconFileName() {
        return "/plugin/artifactory/images/artifactory-icon.png";
    }

    public String getDisplayName() {
        return "Artifactory Build Info";
    }

    public String getUrlName() {
        return RepositoriesUtils.getArtifactoryServer(artifactoryServerName, RepositoriesUtils.getArtifactoryServers()).getUrl() + urlSuffix;
    }

    private String generateUrlSuffix(AbstractProject<?, ?> project) {
        return generateUrlSuffix(ExtractorUtils.sanitizeBuildName(project.getFullName()));
    }

    private String generateUrlSuffix(String buildName) {
        return "/webapp/#/builds/" + Util.rawEncode(buildName) + "/";
    }
}