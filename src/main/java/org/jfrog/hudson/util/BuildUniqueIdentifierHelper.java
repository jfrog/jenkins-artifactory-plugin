package org.jfrog.hudson.util;

import hudson.model.*;
import org.jfrog.hudson.ArtifactoryRedeployPublisher;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.gradle.ArtifactoryGradleConfigurator;

import java.util.logging.Logger;
import org.jfrog.hudson.generic.ArtifactoryGenericConfigurator;


/**
 * Utility class to help extracting and assembling parameters for the a unique build identifier.
 *
 * @author Tomer Cohen
 */
public class BuildUniqueIdentifierHelper {
    private static Logger debuggingLogger = Logger.getLogger(BuildUniqueIdentifierHelper.class.getName());

    private BuildUniqueIdentifierHelper() {
        // utility class
        throw new IllegalAccessError();
    }

    /**
     * Get the root build which triggered the current build. The build root is considered to be the one furthest one
     * away from the current build which has the isPassIdentifiedDownstream active, if no parent build exists, check
     * that the current build needs an upstream identifier, if it does return it.
     *
     * @param currentBuild The current build.
     * @return The root build with isPassIdentifiedDownstream active. Null if no upstream or non is found.
     */
    public static AbstractBuild<?, ?> getRootBuild(AbstractBuild<?, ?> currentBuild) {
        AbstractBuild<?, ?> rootBuild = null;
        AbstractBuild<?, ?> parentBuild = getUpstreamBuild(currentBuild);
        while (parentBuild != null) {
            if (isPassIdentifiedDownstream(parentBuild)) {
                rootBuild = parentBuild;
            }
            parentBuild = getUpstreamBuild(parentBuild);
        }
        if (rootBuild == null && isPassIdentifiedDownstream(currentBuild)) {
            return currentBuild;
        }
        return rootBuild;
    }

    private static AbstractBuild<?, ?> getUpstreamBuild(AbstractBuild<?, ?> build) {
        AbstractBuild<?, ?> upstreamBuild;
        Cause.UpstreamCause cause = ActionableHelper.getUpstreamCause(build);
        if (cause == null) {
            return null;
        }
        AbstractProject<?, ?> upstreamProject = getProject(cause.getUpstreamProject());
        if (upstreamProject == null) {
            debuggingLogger.fine("No project found answering for the name: " + cause.getUpstreamProject());
            return null;
        }
        upstreamBuild = upstreamProject.getBuildByNumber(cause.getUpstreamBuild());
        if (upstreamBuild == null) {
            debuggingLogger.fine(
                    "No build with name: " + upstreamProject.getName() + " and number: " + cause.getUpstreamBuild());
        }
        return upstreamBuild;
    }

    /**
     * Get a project according to its full name.
     *
     * @param fullName The full name of the project.
     * @return The project which answers the full name.
     */
    private static AbstractProject<?, ?> getProject(String fullName) {
        Item item = Hudson.getInstance().getItemByFullName(fullName);
        if (item != null && item instanceof AbstractProject) {
            return (AbstractProject<?, ?>) item;
        }
        return null;
    }

    /**
     * Check whether to pass the the downstream identifier according to the <b>root</b> build's descriptor
     *
     * @param build The current build
     * @return True if to pass the downstream identifier to the child projects.
     */
    public static boolean isPassIdentifiedDownstream(AbstractBuild<?, ?> build) {
        ArtifactoryRedeployPublisher publisher =
                ActionableHelper.getPublisher(build.getProject(), ArtifactoryRedeployPublisher.class);
        if (publisher != null) {
            return publisher.isPassIdentifiedDownstream();
        }
        ArtifactoryGradleConfigurator wrapper = ActionableHelper
                .getBuildWrapper((BuildableItemWithBuildWrappers) build.getProject(),
                        ArtifactoryGradleConfigurator.class);
        if (wrapper != null) {
            return wrapper.isPassIdentifiedDownstream();
        }
        ArtifactoryGenericConfigurator wrapper2 = ActionableHelper
                .getBuildWrapper((BuildableItemWithBuildWrappers) build.getProject(),
                        ArtifactoryGenericConfigurator.class);
        return wrapper2 != null && wrapper2.isPassIdentifiedDownstream();
    }

    /**
     * Get the identifier from the <b>root</b> build. which is composed of {@link hudson.model.AbstractProject#getFullName()}
     * -{@link hudson.model.Run#getNumber()}
     *
     * @param rootBuild The root build
     * @return The upstream identifier.
     */
    public static String getUpstreamIdentifier(AbstractBuild<?, ?> rootBuild) {
        if (rootBuild != null) {
            AbstractProject<?, ?> rootProject = rootBuild.getProject();
            return ExtractorUtils.sanitizeBuildName(rootProject.getFullName()) + "-" + rootBuild.getNumber();
        }

        return null;
    }
}
