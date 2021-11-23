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

package org.jfrog.hudson.maven2;

import com.google.common.collect.Sets;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.MavenArtifact;
import hudson.maven.reporters.MavenArtifactRecord;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.Fingerprinter;
import org.jfrog.build.extractor.builder.ArtifactBuilder;
import org.jfrog.build.extractor.builder.DependencyBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.Artifact;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.hudson.AbstractBuildInfoDeployer;
import org.jfrog.hudson.BuildInfoAwareConfigurator;
import org.jfrog.hudson.JFrogPlatformInstance;
import org.jfrog.hudson.MavenDependenciesRecord;
import org.jfrog.hudson.MavenDependency;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.util.RepositoriesUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the build info. This class is used only when the Maven 3 extractor is not active.
 *
 * @author Yossi Shaul
 */
public class MavenBuildInfoDeployer extends AbstractBuildInfoDeployer {

    private final BuildInfo buildInfo;
    private BuildInfoAwareConfigurator configurator;

    public MavenBuildInfoDeployer(BuildInfoAwareConfigurator configurator, ArtifactoryManager artifactoryManager,
                                  MavenModuleSetBuild build, TaskListener listener) throws IOException, InterruptedException {
        super(configurator, build, listener, artifactoryManager);
        this.configurator = configurator;
        buildInfo = createBuildInfo("Maven", build.getParent().getMaven().getName());
        gatherModuleAndDependencyInfo(build);
    }

    public void deploy() throws IOException {
        JFrogPlatformInstance jfrogServer = RepositoriesUtils.getJFrogPlatformInstances(configurator.getArtifactoryServer().getServerId());
        listener.getLogger().println("Deploying build info to: " + jfrogServer.getArtifactory().getArtifactoryUrl() + "/api/build");
        artifactoryManager.publishBuildInfo(buildInfo, jfrogServer.getUrl());
    }

    private void gatherModuleAndDependencyInfo(MavenModuleSetBuild mavenModulesBuild) {
        Map<MavenModule, MavenBuild> mavenBuildMap = mavenModulesBuild.getModuleLastBuilds();
        List<Module> modules = new ArrayList<>();
        for (Map.Entry<MavenModule, MavenBuild> moduleBuild : mavenBuildMap.entrySet()) {
            MavenModule mavenModule = moduleBuild.getKey();
            MavenBuild mavenBuild = moduleBuild.getValue();
            Result result = mavenBuild.getResult();
            if (Result.NOT_BUILT.equals(result)) {
                // HAP-52 - the module build might be skipped if using incremental build
                continue;
            }
            MavenArtifactRecord mar = ActionableHelper.getLatestMavenArtifactRecord(mavenBuild);
            String moduleId = mavenModule.getName() + ":" + mavenModule.getVersion();
            ModuleBuilder moduleBuilder = new ModuleBuilder().id(moduleId);

            // add artifacts
            moduleBuilder.addArtifact(toArtifact(mar.mainArtifact));
            if (!mar.isPOM() && mar.pomArtifact != null && mar.pomArtifact != mar.mainArtifact) {
                moduleBuilder.addArtifact(toArtifact(mar.pomArtifact));
            }
            for (MavenArtifact attachedArtifact : mar.attachedArtifacts) {
                moduleBuilder.addArtifact(toArtifact(attachedArtifact));
            }

            addDependencies(moduleBuilder, mavenBuild);
            modules.add(moduleBuilder.build());
        }
        buildInfo.setModules(modules);
    }

    private void addDependencies(ModuleBuilder moduleBuilder, MavenBuild mavenBuild) {
        MavenDependenciesRecord dependenciesRecord =
                ActionableHelper.getLatestAction(mavenBuild, MavenDependenciesRecord.class);
        if (dependenciesRecord != null) {
            Set<MavenDependency> dependencies = dependenciesRecord.getDependencies();
            for (MavenDependency dependency : dependencies) {
                DependencyBuilder dependencyBuilder = new DependencyBuilder()
                        .id(dependency.getId())
                        .scopes(Sets.newHashSet(dependency.getScope()))
                        .type(dependency.getType())
                        .md5(getMd5(dependency.getGroupId(), dependency.getFileName(), mavenBuild));
                moduleBuilder.addDependency(dependencyBuilder.build());
            }
            // delete them once used
            build.removeActions(MavenDependenciesRecord.class);
        }
    }

    private Artifact toArtifact(MavenArtifact mavenArtifact) {
        ArtifactBuilder artifactBuilder = new ArtifactBuilder(mavenArtifact.canonicalName)
                .type(mavenArtifact.type).md5(mavenArtifact.md5sum);
        return artifactBuilder.build();
    }

    private String getMd5(String groupId, String fileName, MavenBuild mavenBuild) {
        String md5 = null;
        Fingerprinter.FingerprintAction fingerprint = ActionableHelper.getLatestAction(
                mavenBuild, Fingerprinter.FingerprintAction.class);
        if (fingerprint != null) {
            md5 = fingerprint.getRecords().get(groupId + ":" + fileName);
        }
        return md5;
    }
}
