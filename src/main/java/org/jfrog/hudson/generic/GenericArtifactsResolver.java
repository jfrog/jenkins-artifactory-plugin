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

import hudson.EnvVars;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryDependenciesClient;
import org.jfrog.build.util.BuildDependenciesHelper;
import org.jfrog.build.util.DependenciesDownloader;
import org.jfrog.build.util.DependenciesHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.util.ExtractorUtils;

/**
 * Resolves artifacts from Artifactory (published dependencies and build
 * dependencies) This class is used only in free style generic configurator.
 *
 * @author Shay Yaakov
 */
public class GenericArtifactsResolver {

    private final AbstractBuild build;
    private ArtifactoryGenericConfigurator configurator;
    private final ArtifactoryDependenciesClient client;
    private String resolvePattern;
    private Log log;
    private EnvVars env;


    public GenericArtifactsResolver(AbstractBuild build, ArtifactoryGenericConfigurator configurator, BuildListener listener, ArtifactoryDependenciesClient client, String resolvePattern) throws IOException, InterruptedException {
        this.build = build;
        this.client = client;
        this.configurator = configurator;
        this.env = build.getEnvironment(listener);
        this.resolvePattern = Util.replaceMacro(resolvePattern, build.getEnvironment(listener));
        log = new JenkinsBuildInfoLog(listener);
    }

    public List<Dependency> retrievePublishedDependencies() throws IOException, InterruptedException {
        DependenciesHelper helper = new DependenciesHelper(createDependenciesDownloader(), log);
        Cause.UpstreamCause parent = ActionableHelper.getUpstreamCause(build);
        if (parent != null) {
            env.put("BUILD_PARENTNAME", ExtractorUtils.sanitizeBuildName(parent.getUpstreamProject()));
            env.put("BUILD_PARENTNUMBER", parent.getUpstreamBuild() + "");
        } else {
            env.put("BUILD_PARENTNUMBER", "LATEST");
        }

        String resolvePattern = Util.replaceMacro(configurator.getResolvePattern(), env);
        //resolvePattern = StringUtils.replace(resolvePattern, "\r\n", "\n");
        //resolvePattern = StringUtils.replace(resolvePattern, ",", "\n");
        return helper.retrievePublishedDependencies(resolvePattern);
    }

    public List<BuildDependency> retrieveBuildDependencies() throws IOException, InterruptedException {
        BuildDependenciesHelper helper = new BuildDependenciesHelper(createDependenciesDownloader(), log);
        Cause.UpstreamCause parent = ActionableHelper.getUpstreamCause(build);
        if (parent != null) {
            env.put("BUILD_PARENTNAME", ExtractorUtils.sanitizeBuildName(parent.getUpstreamProject()));
            env.put("BUILD_PARENTNUMBER", parent.getUpstreamBuild() + "");
        } else {
            env.put("BUILD_PARENTNUMBER", "LATEST");
        }
        String resolvePattern = Util.replaceMacro(configurator.getResolvePattern(), env);
        //resolvePattern = StringUtils.replace(resolvePattern, "\r\n", "\n");
        //resolvePattern = StringUtils.replace(resolvePattern, ",", "\n");
        return helper.retrieveBuildDependencies(resolvePattern);
    }

    private DependenciesDownloader createDependenciesDownloader() {
        return new DependenciesDownloaderImpl(client, build.getWorkspace(), log);
    }
}
