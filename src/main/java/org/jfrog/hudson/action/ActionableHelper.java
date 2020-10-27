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

import com.google.common.collect.Lists;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixConfiguration;
import hudson.maven.MavenBuild;
import hudson.maven.reporters.MavenArtifactRecord;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.hudson.DeployerOverrider;
import org.jfrog.hudson.ResolverOverrider;
import org.jfrog.hudson.util.publisher.PublisherFindImpl;
import org.jfrog.hudson.util.publisher.PublisherFlexible;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author Yossi Shaul
 */
public abstract class ActionableHelper implements Serializable {
    private static final long serialVersionUID = 1L;

    public static MavenArtifactRecord getLatestMavenArtifactRecord(MavenBuild mavenBuild) {
        return getLatestAction(mavenBuild, MavenArtifactRecord.class);
    }

    /**
     * Returns the latest action of the type. One module may produce multiple action entries of the same type, in some
     * cases the last one contains all the info we need (previous ones might only contain partial information, eg, only
     * main artifact)
     *
     * @param build       The build
     * @param actionClass The type of the action
     * @return Latest action of the given type or null if not found
     */
    public static <T extends Action> T getLatestAction(Run build, Class<T> actionClass) {
        List<T> records = build.getActions(actionClass);
        if (records == null || records.isEmpty()) {
            return null;
        } else {
            return records.get(records.size() - 1);
        }
    }


    /**
     * Search for a publisher of the given type in a project and return it, or null if it is not found.
     *
     * @return The publisher
     */
    public static <T extends Publisher> T getPublisher(AbstractProject<?, ?> project, Class<T> type) {
        // Search for a publisher of the given type in the project and return it if found:
        T publisher = new PublisherFindImpl<T>().find(project, type);
        if (publisher != null) {
            return publisher;
        }
        // If not found, the publisher might be wrapped by a "Flexible Publish" publisher. The below searches for it inside the
        // Flexible Publisher:
        publisher = new PublisherFlexible<T>().find(project, type);
        return publisher;
    }

    /**
     * @return The wrapped item (eg, project) wrapper of the given type. Null if not found.
     */
    private static <T> T getBuildableItem(BuildableItem wrapped, Class<T> type) {
        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappers =
                ((BuildableItemWithBuildWrappers) wrapped).getBuildWrappersList();
        for (BuildWrapper wrapper : wrappers) {
            if (type.isInstance(wrapper)) {
                return type.cast(wrapper);
            }
        }
        return null;
    }

    /**
     * @return The wrapped item (eg, project) wrapper of the given type. Null if not found.
     */
    public static <T extends BuildWrapper> T getBuildWrapper(BuildableItem wrapped, Class<T> type) {
        return getBuildableItem(wrapped, type);
    }

    /**
     * @return The wrapped item (eg, project) which implements the DeployerOverrider interface.
     * Null is returned if not found.
     */
    public static DeployerOverrider getDeployerOverrider(BuildableItem wrapped) {
        return getBuildableItem(wrapped, DeployerOverrider.class);
    }

    /**
     * @return The wrapped item (eg, project) which implements the ResolverOverrider interface.
     * Null is returned if not found.
     */
    public static ResolverOverrider getResolverOverrider(BuildableItem wrapped) {
        return getBuildableItem(wrapped, ResolverOverrider.class);
    }

    /**
     * Get a list of {@link Builder}s that are related to the project.
     *
     * @param project The project from which to get the builder.
     * @param type    The type of the builder (the actual class)
     * @param <T>     The type that the class represents
     * @return A list of builders that answer the class definition that are attached to the project.
     */
    public static <T extends Builder> List<T> getBuilder(Project<?, ?> project, Class<T> type) {
        List<T> result = Lists.newArrayList();
        DescribableList<Builder, Descriptor<Builder>> builders = project.getBuildersList();
        for (Builder builder : builders) {
            if (type.isInstance(builder)) {
                result.add(type.cast(builder));
            }
        }
        return result;
    }

    public static Cause.UpstreamCause getUpstreamCause(Run build) {
        CauseAction action = ActionableHelper.getLatestAction(build, CauseAction.class);
        if (action != null) {
            for (Cause cause : action.getCauses()) {
                if (cause instanceof Cause.UpstreamCause) {
                    return (Cause.UpstreamCause) cause;
                }
            }
        }
        return null;
    }

    /**
     * @param build The build
     * @return The user id caused triggered the build. "anonymous" if not started by a user
     */
    public static String getUserCausePrincipal(Run build) {
        return getUserCausePrincipal(build, "anonymous");
    }

    /**
     * @param build            The build
     * @param defaultPrincipal Principal to return if the user who caused the id is not found
     * @return The user id caused triggered the build of default principal if not found
     */
    public static String getUserCausePrincipal(Run build, String defaultPrincipal) {
        Cause.UserIdCause userCause = getUserCause(build);
        String principal = defaultPrincipal;
        if (userCause != null && userCause.getUserId() != null) {
            principal = userCause.getUserId();
        }
        return principal;
    }

    private static Cause.UserIdCause getUserCause(Run build) {
        CauseAction action = ActionableHelper.getLatestAction(build, CauseAction.class);
        if (action != null) {
            for (Cause cause : action.getCauses()) {
                if (cause instanceof Cause.UserIdCause) {
                    return (Cause.UserIdCause) cause;
                }
            }
        }
        return null;
    }

    public static String getBuildUrl(Run build) {
        String root = Hudson.get().getRootUrl();
        if (StringUtils.isBlank(root)) {
            return "";
        }
        return root + build.getUrl();
    }

    /**
     * Return list with {@link ArtifactoryProjectAction} if not already exists in project actions.
     *
     * @param artifactoryServerName The name of Artifactory server
     * @param project               The hudson project
     * @return Empty list or list with one {@link ArtifactoryProjectAction}
     */
    public static List<ArtifactoryProjectAction> getArtifactoryProjectAction(
            String artifactoryServerName, AbstractProject project) {
        if (shouldReturnEmptyList(artifactoryServerName, project)) return Collections.emptyList();

        return Lists.newArrayList(new ArtifactoryProjectAction(artifactoryServerName, project));
    }

    private static boolean shouldReturnEmptyList(String artifactoryServerName, AbstractProject project) {
        if (artifactoryServerName == null) {
            return true;
        }
        if (project.getAction(ArtifactoryProjectAction.class) != null) {
            // don't add if already exist (if multiple Artifactory builders are configured in free style)
            return true;
        }
        if (project instanceof MatrixConfiguration)
            return true;
        return false;
    }

    /**
     * Return list with {@link ArtifactoryProjectAction} if not already exists in project actions.
     *
     * @param artifactoryServerName The name of Artifactory server
     * @param project               The hudson project
     * @return Empty list or list with one {@link ArtifactoryProjectAction}
     */
    public static List<ArtifactoryProjectAction> getArtifactoryProjectAction(
            String artifactoryServerName, AbstractProject project, String buildName) {
        if (shouldReturnEmptyList(artifactoryServerName, project)) {
            return Collections.emptyList();
        }

        return Lists.newArrayList(new ArtifactoryProjectAction(artifactoryServerName, buildName));
    }

    /**
     * Returns the version of Jenkins Artifactory Plugin or empty string if not found
     *
     * @return the version of Jenkins Artifactory Plugin or empty string if not found
     */
    public static String getArtifactoryPluginVersion() {
        String pluginsSortName = "artifactory";
        //Validates Jenkins existence because in some jobs the Jenkins instance is unreachable
        if (Jenkins.get() != null
                && Jenkins.get().getPlugin(pluginsSortName) != null
                && Jenkins.get().getPlugin(pluginsSortName).getWrapper() != null) {
            return Jenkins.get().getPlugin(pluginsSortName).getWrapper().getVersion();
        }
        return "";
    }

    /**
     * Returns the default number of retries
     *
     * @return the default number of retries
     */
    public static int getDefaultConnectionRetries() {
        return ArtifactoryHttpClient.DEFAULT_CONNECTION_RETRY;
    }

    /**
     * Deletes a FilePath file.
     *
     * @param workspace The build workspace.
     * @param path      The path in the workspace.
     * @throws IOException In case of missing file.
     */
    public static void deleteFilePath(FilePath workspace, String path) throws IOException {
        if (StringUtils.isNotBlank(path)) {
            try {
                FilePath propertiesFile = new FilePath(workspace, path);
                propertiesFile.delete();
            } catch (Exception e) {
                throw new IOException("Could not delete temp file: " + path);
            }
        }
    }

    /**
     * Deletes a FilePath file on exit.
     *
     * @param workspace The build workspace.
     * @param path      The path in the workspace.
     * @throws IOException In case of a missing file.
     */
    public static void deleteFilePathOnExit(FilePath workspace, String path) throws IOException, InterruptedException {
        FilePath filePath = new FilePath(workspace, path);
        deleteFilePathOnExit(filePath);
    }

    /**
     * Deletes a FilePath file on exit.
     *
     * @param filePath The FilePath to delete on exit.
     * @throws IOException In case of a missing file.
     */
    public static void deleteFilePathOnExit(FilePath filePath) throws IOException, InterruptedException {
        filePath.act(new MasterToSlaveFileCallable<Void>() {
            public Void invoke(File file, VirtualChannel virtualChannel) throws IOException {
                FileUtils.forceDeleteOnExit(file);
                return null;
            }
        });
    }

    public static Node getNode(Launcher launcher) {
        Node node = null;
        Jenkins j = Jenkins.get();
        for (Computer c : j.getComputers()) {
            if (c.getChannel() == launcher.getChannel()) {
                node = c.getNode();
                break;
            }
        }
        return node;
    }
}
