/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.hudson.release.maven;

import com.google.common.collect.Maps;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.ModuleName;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.maven.transformer.SnapshotNotAllowedException;
import org.jfrog.hudson.ArtifactoryRedeployPublisher;
import org.jfrog.hudson.BintrayPublish.BintrayPublishAction;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.release.UnifiedPromoteBuildAction;
import org.jfrog.hudson.release.scm.AbstractScmCoordinator;
import org.jfrog.hudson.release.scm.ScmCoordinator;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A release wrapper for maven projects. Allows performing release steps on maven
 *
 * @author Yossi Shaul
 */
public class MavenReleaseWrapper extends BuildWrapper {
    private static final Logger debuggingLogger = Logger.getLogger(MavenReleaseWrapper.class.getName());

    private String tagPrefix;
    private String releaseBranchPrefix;
    private String targetRemoteName;
    private String alternativeGoals;
    private String defaultVersioning;
    private String defaultReleaseStagingRepository;

    private transient ScmCoordinator scmCoordinator;

    @DataBoundConstructor
    public MavenReleaseWrapper(String releaseBranchPrefix, String tagPrefix, String targetRemoteName, String alternativeGoals,
                               String defaultVersioning, String defaultReleaseStagingRepository) {
        this.releaseBranchPrefix = releaseBranchPrefix;
        this.tagPrefix = tagPrefix;
        this.targetRemoteName = targetRemoteName;
        this.alternativeGoals = alternativeGoals;
        this.defaultVersioning = defaultVersioning;
        this.defaultReleaseStagingRepository = defaultReleaseStagingRepository;
    }

    public String getTagPrefix() {
        return tagPrefix;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTagPrefix(String tagPrefix) {
        this.tagPrefix = tagPrefix;
    }

    public String getTargetRemoteName() {
        return targetRemoteName;
    }

    public void setTargetRemoteName(String targetRemoteName) {
        this.targetRemoteName = targetRemoteName;
    }

    public String getReleaseBranchPrefix() {
        return releaseBranchPrefix;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setReleaseBranchPrefix(String releaseBranchPrefix) {
        this.releaseBranchPrefix = releaseBranchPrefix;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getAlternativeGoals() {
        return alternativeGoals;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setAlternativeGoals(String alternativeGoals) {
        this.alternativeGoals = alternativeGoals;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getDefaultVersioning() {
        return defaultVersioning;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setDefaultVersioning(String defaultVersioning) {
        this.defaultVersioning = defaultVersioning;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getDefaultReleaseStagingRepository() {
        return defaultReleaseStagingRepository;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setDefaultReleaseStagingRepository(String defaultReleaseStagingRepository) {
        this.defaultReleaseStagingRepository = defaultReleaseStagingRepository;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {

        final ReleaseAction releaseAction = build.getAction(ReleaseAction.class);
        if (releaseAction == null) {
            // this is a normal non release build, continue with normal environment
            return new Environment() {
            };
        }

        log(listener, "Release build triggered");

        final MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;
        if(!releaseAction.isValid(build, listener, releaseAction.getVersioning().name())){
            return null;
        }
        scmCoordinator = AbstractScmCoordinator.createScmCoordinator(build, listener, releaseAction);
        scmCoordinator.prepare();
        if (!releaseAction.getVersioning().equals(ReleaseAction.VERSIONING.NONE)) {
            scmCoordinator.beforeReleaseVersionChange();
            // change to release version
            String vcsUrl = releaseAction.isCreateVcsTag() && AbstractScmCoordinator.isSvn(build.getProject())
                    ? releaseAction.getTagUrl() : null;
            boolean modified;
            try {
                log(listener, "Changing POMs to release version");
                modified = changeVersions(mavenBuild, releaseAction, true, vcsUrl);
            } catch (SnapshotNotAllowedException e) {
                log(listener, "ERROR: " + e.getMessage());
                // abort the build
                return null;
            }
            scmCoordinator.afterReleaseVersionChange(modified);
        }

        final MavenModuleSet mavenModuleSet = mavenBuild.getProject();
        final String originalGoals = mavenModuleSet.getGoals();
        if (!StringUtils.isBlank(alternativeGoals)) {
            debuggingLogger.fine("Using alternative goals and settings: " + alternativeGoals);
            mavenModuleSet.setGoals(alternativeGoals);
        }

        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException {
                // if we used alternative goals set back the original
                if (!StringUtils.isBlank(alternativeGoals)) {
                    mavenModuleSet.setGoals(originalGoals);
                }

                if (build.getResult().isWorseThan(Result.SUCCESS)) {
                    // revert will happen by the listener
                    return true;
                }

                try {
                    scmCoordinator.afterSuccessfulReleaseVersionBuild();

                    if (!releaseAction.getVersioning().equals(ReleaseAction.VERSIONING.NONE)) {
                        scmCoordinator.beforeDevelopmentVersionChange();
                        // change poms versions to next development version
                        String scmUrl = releaseAction.isCreateVcsTag() &&
                                AbstractScmCoordinator.isSvn(build.getProject())
                                ? scmCoordinator.getRemoteUrlForPom() : null;
                        log(listener, "Changing POMs to next development version");
                        boolean modified = changeVersions(mavenBuild, releaseAction, false, scmUrl);
                        scmCoordinator.afterDevelopmentVersionChange(modified);
                    }
                } catch (Exception e) {
                    listener.getLogger().println("Failure in post build SCM action: " + e.getMessage());
                    debuggingLogger.log(Level.FINE, "Failure in post build SCM action: ", e);
                    return false;
                }
                return true;
            }
        };
    }

    private boolean changeVersions(MavenModuleSetBuild mavenBuild, ReleaseAction release, boolean releaseVersion,
                                   String scmUrl) throws IOException, InterruptedException {
        FilePath moduleRoot = mavenBuild.getModuleRoot();
        // get the active modules only
        Collection<MavenModule> modules = mavenBuild.getProject().getDisabledModules(false);

        Map<ModuleName, String> modulesByName = Maps.newHashMap();
        for (MavenModule module : modules) {
            String version = releaseVersion ? release.getReleaseVersionFor(module.getModuleName()) :
                    release.getNextVersionFor(module.getModuleName());
            modulesByName.put(module.getModuleName(), version);
        }

        boolean modified = false;
        for (MavenModule mavenModule : modules) {
            String relativePath = mavenModule.getRelativePath();
            String pomRelativePath = StringUtils.isBlank(relativePath) ? "pom.xml" : relativePath + "/pom.xml";
            FilePath pomPath = new FilePath(moduleRoot, pomRelativePath);
            debuggingLogger.fine("Changing version of pom: " + pomPath);
            scmCoordinator.edit(pomPath);
            modified |= pomPath.act(
                    new PomTransformer(mavenModule.getModuleName(), modulesByName, scmUrl, releaseVersion));
        }
        return modified;
    }

    private void log(BuildListener listener, String message) {
        listener.getLogger().println("[RELEASE] " + message);
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject job) {
        MavenModuleSet moduleSet = (MavenModuleSet)job;
        return Arrays.asList(
                new MavenReleaseAction(moduleSet),
                new MavenReleaseApiAction(moduleSet)
        );
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            super(MavenReleaseWrapper.class);
            load();
        }

        /**
         * This wrapper applied to maven projects with subversion only.
         *
         * @param item The current project
         * @return True for maven projects with subversion as the scm
         */
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return (item instanceof MavenModuleSet)/* && (item.getScm() instanceof SubversionSCM)*/;
        }

        /**
         * @return The message to be displayed next to the checkbox in the job configuration.
         */
        @Override
        public String getDisplayName() {
            return "Enable Artifactory release management";
        }

        /**
         * @return Model with the release actions allowed. Used to set the defaultVersioning.
         */
        @SuppressWarnings({"UnusedDeclaration"})
        public ListBoxModel doFillDefaultVersioningItems() {
            ListBoxModel model = new ListBoxModel();
            for (ReleaseAction.VERSIONING versioning : ReleaseAction.VERSIONING.values()) {
                model.add(versioning.getDisplayMessage(), versioning.toString());
            }
            return model;
        }
    }

    /**
     * This run listener handles the job completed event to cleanup svn tags and working copy in case of build failure.
     */
    @Extension
    public static final class ReleaseRunListener extends RunListener<AbstractBuild> {
        /**
         * Completed event is sent after the build and publishers execution. The build result in this stage is final and
         * cannot be modified. So this is a good place to revert working copy and tag if the build failed.
         */
        @Override
        public void onCompleted(AbstractBuild run, TaskListener listener) {
            if (!(run instanceof MavenModuleSetBuild)) {
                return;
            }

            ReleaseAction releaseAction = run.getAction(ReleaseAction.class);
            if (releaseAction == null) {
                return;
            }

            // signal completion to the scm coordinator
            MavenModuleSet project = ((MavenModuleSetBuild) run).getProject();

            ArtifactoryRedeployPublisher redeployPublisher =
                    ActionableHelper.getPublisher(project, ArtifactoryRedeployPublisher.class);
            Result result = run.getResult();
            boolean successRun = result.isBetterOrEqualTo(Result.SUCCESS);
            if (successRun) {
                if (!redeployPublisher.isAllowPromotionOfNonStagedBuilds()) {
                    // add a stage action
                    run.addAction(new UnifiedPromoteBuildAction<ArtifactoryRedeployPublisher>(run, redeployPublisher));
                }
                // Checks if Push to Bintray is disabled.
                if (PluginsUtils.isPushToBintrayEnabled()) {
                    if (!redeployPublisher.isAllowBintrayPushOfNonStageBuilds()) {
                        run.addAction(new BintrayPublishAction<ArtifactoryRedeployPublisher>(run, redeployPublisher));
                    }
                }
            }

            MavenReleaseWrapper wrapper = ActionableHelper.getBuildWrapper(project, MavenReleaseWrapper.class);
            try {
                wrapper.scmCoordinator.buildCompleted();
            } catch (Exception e) {
                run.setResult(Result.FAILURE);
                listener.error("[RELEASE] Failed on build completion");
                e.printStackTrace(listener.getLogger());
            }

            // remove the release action from the build. the stage action is the point of interaction for successful builds
            run.getActions().remove(releaseAction);
        }
    }
}
