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

package org.jfrog.hudson.generic;

import com.google.common.collect.Lists;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.retention.Utils;
import org.jfrog.hudson.AbstractBuildInfoDeployer;
import org.jfrog.hudson.util.BuildRetentionFactory;
import org.jfrog.hudson.util.ExtractorUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Builds the build info for generic deployment
 *
 * @author Yossi Shaul
 */
public class GenericBuildInfoDeployer extends AbstractBuildInfoDeployer {

    private final Run build;
    private ArtifactoryGenericConfigurator configurator;
    private Build buildInfo;

    public GenericBuildInfoDeployer(ArtifactoryGenericConfigurator configurator, ArtifactoryBuildInfoClient client,
                                    Run build, TaskListener listener, List<Artifact> deployedArtifacts,
                                    List<BuildDependency> buildDependencies, List<Dependency> publishedDependencies)
            throws IOException, NoSuchAlgorithmException, InterruptedException {
        super(configurator, build, listener, client);
        this.configurator = configurator;
        this.build = build;
        this.buildInfo = createBuildInfo("Generic", "");
        createDeployDetailsAndAddToBuildInfo(deployedArtifacts, publishedDependencies);
    }

    public void deploy() throws IOException {
        String url = configurator.getArtifactoryServer().getArtifactoryUrl() + "/api/build";
        listener.getLogger().println("Deploying build info to: " + url);
        BuildRetention retention = getBuildRetention();
        Utils.sendBuildAndBuildRetention(client, buildInfo, retention, configurator.isAsyncBuildRetention());
    }

    private BuildRetention getBuildRetention() {
        BuildRetention buildRetention = null;
        if (configurator.isDiscardOldBuilds()) {
            buildRetention = BuildRetentionFactory.createBuildRetention(build, configurator.isDiscardBuildArtifacts());
        }
        return buildRetention;
    }

    private void createDeployDetailsAndAddToBuildInfo(List<Artifact> deployedArtifacts,
                                                      List<Dependency> publishedDependencies) throws IOException, NoSuchAlgorithmException {
        ModuleBuilder moduleBuilder =
                new ModuleBuilder().id(
                        ExtractorUtils.sanitizeBuildName(build.getParent().getDisplayName()) + ":" + build.getNumber())
                        .artifacts(deployedArtifacts);
        moduleBuilder.dependencies(publishedDependencies);
        buildInfo.setModules(Lists.newArrayList(moduleBuilder.build()));
    }
}
