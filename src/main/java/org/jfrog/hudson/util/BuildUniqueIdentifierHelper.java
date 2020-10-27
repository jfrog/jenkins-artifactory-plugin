package org.jfrog.hudson.util;

import hudson.matrix.Combination;
import hudson.matrix.MatrixRun;
import hudson.model.*;
import org.jfrog.hudson.ArtifactoryRedeployPublisher;
import org.jfrog.hudson.BuildInfoAwareConfigurator;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.gradle.ArtifactoryGradleConfigurator;

import java.util.logging.Logger;


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
        Item item = Hudson.get().getItemByFullName(fullName);
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
        return wrapper != null && wrapper.isPassIdentifiedDownstream();
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

    public static String getBuildName(Run build) {
        String lastItemInBuildName = build.getParent().getName();
        String buildName = build.getParent().getFullName();
        if (!buildName.equals(lastItemInBuildName)) {
            if (build instanceof MatrixRun) {
                // If we are using Multi-configuration plugin we want to omit the parameters in the end of the build name
                int stringToBeOmittedIndex = buildName.indexOf(lastItemInBuildName);
                if (stringToBeOmittedIndex > 0) {
                    buildName = buildName.substring(0, stringToBeOmittedIndex - 1);
                }
            }
        }
        return ExtractorUtils.sanitizeBuildName(buildName);
    }

    public static String getBuildNumber(Run build) {
        String buildNumber = String.valueOf(build.getNumber());
        if (build instanceof MatrixRun) {
            buildNumber += " :: ";
            Combination combination = ((MatrixRun) build).getProject().getCombination();
            buildNumber += combination.toString();
        }

        return buildNumber;
    }

    public static String getBuildNameConsiderOverride(BuildInfoAwareConfigurator configurator, Run build) {
        return configurator.isOverrideBuildName() ? configurator.getCustomBuildName() : BuildUniqueIdentifierHelper.getBuildName(build);
    }
}
