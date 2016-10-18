/*
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson.generic;

import com.google.common.collect.Lists;
import hudson.EnvVars;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.*;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;
import java.util.List;

/**
 * Resolves artifacts from Artifactory (published dependencies and build dependencies)
 * This class is used only in free style generic configurator.
 *
 * @author Shay Yaakov
 */
public class GenericArtifactsResolver {
    private final AbstractBuild build;
    private final ArtifactoryDependenciesClient client;
    private Log log;
    private EnvVars envVars;

    public GenericArtifactsResolver(AbstractBuild build, BuildListener listener, ArtifactoryDependenciesClient client)
            throws IOException, InterruptedException {
        this.build = build;
        this.client = client;
        this.envVars = build.getEnvironment(listener);
        this.log = new JenkinsBuildInfoLog(listener);
    }

    public List<Dependency> retrievePublishedDependencies(String resolvePattern) throws IOException, InterruptedException {
        DependenciesHelper helper = new AntPatternsDependenciesHelper(createDependenciesDownloader(), log);
        return helper.retrievePublishedDependencies(replaceVariables(resolvePattern));
    }

    public List<BuildDependency> retrieveBuildDependencies(String resolvePattern) throws IOException, InterruptedException {
        BuildDependenciesHelper helper = new BuildDependenciesHelper(createDependenciesDownloader(), log);
        return helper.retrieveBuildDependencies(replaceVariables(resolvePattern));
    }

    public List<Dependency> retrieveDependenciesBySpec(String serverUrl, String downloadSpec) throws IOException {
        if (StringUtils.isEmpty(downloadSpec)) {
            return Lists.newArrayList();
        }
        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(createDependenciesDownloader(), log);
        SpecsHelper specsHelper = new SpecsHelper(log);
        Spec spec = specsHelper.getDownloadUploadSpec(replaceVariables(downloadSpec));
        return helper.downloadDependencies(serverUrl, spec);
    }

    private DependenciesDownloader createDependenciesDownloader() {
        return new DependenciesDownloaderImpl(client, build.getWorkspace(), log);
    }

    private String replaceVariables(String subject) {
        return Util.replaceMacro(subject, envVars);
    }
}
