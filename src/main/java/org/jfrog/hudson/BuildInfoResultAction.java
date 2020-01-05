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

package org.jfrog.hudson;

import hudson.Util;
import hudson.model.BuildBadgeAction;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Result of the redeploy publisher. Currently only a link to Artifactory build info.
 *
 * @author Yossi Shaul
 */
public class BuildInfoResultAction implements BuildBadgeAction {

    private List<PublishedBuildDetails> publishedBuildsDetails = new CopyOnWriteArrayList<>();
    private final Run build;
    @Deprecated
    private String url;
    /**
     * @deprecated Only here to keep compatibility with version 1.0.7 and below (part of the xstream de-serialization)
     */
    @Deprecated
    private transient ArtifactoryRedeployPublisher artifactoryRedeployPublisher;

    public BuildInfoResultAction(Run build) {
        this.build = build;
    }

    public BuildInfoResultAction(String artifactoryUrl, Run build, String buildName) {
        this(build);
        String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
        publishedBuildsDetails.add(createBuildInfoIdentifier(artifactoryUrl, buildName, buildNumber));
    }

    public void addBuildInfoResults(String artifactoryUrl, Build buildInfo) {
        publishedBuildsDetails.add(createBuildInfoIdentifier(artifactoryUrl, build, buildInfo));
    }

    public Run getBuild() {
        return build;
    }

    public String getIconFileName() {
        return "/plugin/artifactory/images/artifactory-icon.png";
    }

    public String getDisplayName() {
        return "Artifactory Build Info";
    }

    public String getUrlName() {
        if (StringUtils.isNotEmpty(url)) {
            return url;
        } else if (publishedBuildsDetails == null) {
            publishedBuildsDetails = new CopyOnWriteArrayList<>();
        }
        // For backward compatibility if publishedBuildsDetails is empty calculate it from the old structs.
        if (publishedBuildsDetails.size() == 0 && artifactoryRedeployPublisher != null && build != null) {
            String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(artifactoryRedeployPublisher, build);
            return generateUrl(artifactoryRedeployPublisher.getArtifactoryName(), build, buildName);
        } else if (publishedBuildsDetails.size() == 1) {
            return publishedBuildsDetails.get(0).getBuildInfoUrl();
        }
        return "published_builds";
    }

    private PublishedBuildDetails createBuildInfoIdentifier(String artifactoryUrl, String buildName, String buildNumber) {
        return new PublishedBuildDetails(artifactoryUrl, Util.rawEncode(buildName), Util.rawEncode(buildNumber));
    }

    private PublishedBuildDetails createBuildInfoIdentifier(String artifactoryUrl, Run build, Build buildInfo) {
        String buildName;
        String buildNumber;
        if (StringUtils.isNotEmpty(buildInfo.getName())) {
            buildName = buildInfo.getName();
        } else {
            buildName = BuildUniqueIdentifierHelper.getBuildName(build);
        }
        if (StringUtils.isNotEmpty(buildInfo.getNumber())) {
            buildNumber = buildInfo.getNumber();
        } else {
            buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
        }
        return createBuildInfoIdentifier(artifactoryUrl, buildName, buildNumber);
    }

    private String generateUrl(String artifactoryUrl, Run build, String buildName) {
        return artifactoryUrl + "/webapp/builds/" + Util.rawEncode(buildName) + "/"
                + Util.rawEncode(BuildUniqueIdentifierHelper.getBuildNumber(build));
    }

    /**
     * @return whether we have multiple builds. Called from the UI.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public boolean haveMultipleBuilds() {
        return publishedBuildsDetails != null && publishedBuildsDetails.size() > 1;
    }

    /**
     * @return list of build info identifiers. Called from the UI.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public List<PublishedBuildDetails> getPublishedBuildsDetails() {
        return publishedBuildsDetails;
    }

    @Deprecated
    public void setUrl(String url) {
        this.url = url;
    }
}