package org.jfrog.hudson.maven3;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.util.ListBoxModel;
import hudson.util.XStream2;
import org.jfrog.hudson.*;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.util.*;
import org.jfrog.hudson.util.converters.DeployerResolverOverriderConverter;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A wrapper that takes over artifacts resolution and using the configured repository for resolution.<p/>
 * The {@link org.jfrog.hudson.maven3.Maven3ExtractorListener} is doing the heavy lifting. This class now just holds
 * the configuration.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryMaven3NativeConfigurator extends BuildWrapper implements ResolverOverrider {

    private final ServerDetails resolverDetails;
    /**
     * @deprecated: Use org.jfrog.hudson.maven3.ArtifactoryMaven3NativeConfigurator#getResolverCredentialsId()()
     */
    @Deprecated
    private Credentials overridingResolverCredentials;
    private CredentialsConfig resolverCredentialsConfig;

    /**
     * @deprecated: The following deprecated variables have corresponding converters to the variables replacing them
     */
    @Deprecated
    private ServerDetails details = null;

    @DataBoundConstructor
    public ArtifactoryMaven3NativeConfigurator(ServerDetails details, ServerDetails resolverDetails, CredentialsConfig resolverCredentialsConfig) {
        this.resolverDetails = resolverDetails;
        this.resolverCredentialsConfig = resolverCredentialsConfig;
    }

    /**
     * Constructor for the DeployerResolverOverriderConverterTest
     *
     * @param details         - Old server details
     * @param resolverDetails - new resolver details
     */
    public ArtifactoryMaven3NativeConfigurator(ServerDetails details, ServerDetails resolverDetails) {
        this.details = details;
        this.resolverDetails = resolverDetails;
    }

    public ServerDetails getDeployerDetails() {
        return getResolverDetails();
    }

    public ServerDetails getResolverDetails() {
        return resolverDetails;
    }

    public String getDownloadReleaseRepositoryKey() {
        return getDeployerDetails() != null ? getDeployerDetails().getResolveReleaseRepository().getRepoKey() : null;
    }

    public String getDownloadSnapshotRepositoryKey() {
        return getDeployerDetails() != null ? getDeployerDetails().getResolveSnapshotRepositoryKey() : null;
    }

    public String getArtifactoryName() {
        return getDeployerDetails() != null ? getDeployerDetails().artifactoryName : null;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject project) {
        return Collections.emptyList();
    }

    public boolean isOverridingDefaultResolver() {
        return resolverCredentialsConfig != null && resolverCredentialsConfig.isCredentialsProvided();
    }

    public Credentials getOverridingResolverCredentials() {
        return overridingResolverCredentials;
    }

    public CredentialsConfig getResolverCredentialsConfig() {
        return resolverCredentialsConfig;
    }

    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        if (!(build instanceof MavenModuleSetBuild)) {
            return new Environment() {
            };
        }

        PrintStream log = listener.getLogger();
        log.println("Jenkins Artifactory Plugin version: " + ActionableHelper.getArtifactoryPluginVersion());
        EnvVars envVars = build.getEnvironment(listener);
        boolean supportedMavenVersion =
                MavenVersionHelper.isAtLeastResolutionCapableVersion((MavenModuleSetBuild) build, envVars, listener);
        if (!supportedMavenVersion) {
            log.println("Artifactory resolution is not active. Maven 3.0.2 or higher is required to " +
                    "force resolution from Artifactory.");
            return new Environment() {
            };
        }

        /**
         * {@link org.jfrog.hudson.maven3.Maven3ExtractorListener} will populate the resolver context
         * */
        return new Environment() {
        };
    }

    public ArtifactoryServer getArtifactoryServer() {
        List<ArtifactoryServer> servers = getDescriptor().getArtifactoryServers();
        for (ArtifactoryServer server : servers) {
            if (server.getServerId().equals(getArtifactoryName())) {
                return server;
            }
        }
        return null;
    }

    public List<VirtualRepository> getVirtualRepositoryList() {
        String releaseRepoKey = getDeployerDetails().getResolveReleaseRepository().getKeyFromSelect();
        String snapshotRepoKey = getDeployerDetails().getResolveSnapshotRepository().getKeyFromSelect();

        // Add the releases repo to the reposities list, in case it is not there:
        List<VirtualRepository> repos = RepositoriesUtils.collectVirtualRepositories(null, releaseRepoKey);

        // Add the snapshots repo to the reposities list, in case it is not there:
        return RepositoriesUtils.collectVirtualRepositories(repos, snapshotRepoKey);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    private boolean isExtractorUsed(EnvVars env) {
        return Boolean.parseBoolean(env.get(ExtractorUtils.EXTRACTOR_USED));
    }

    @Extension
    public static class DescriptorImpl extends AbstractBuildWrapperDescriptor {
        private static final String DISPLAY_NAME = "Resolve artifacts from Artifactory";
        private static final String CONFIG_PREFIX = "maven";

        public DescriptorImpl() {
            super(ArtifactoryMaven3NativeConfigurator.class, DISPLAY_NAME, CONFIG_PREFIX);
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            this.item = item;
            Class<?> itemClass = item.getClass();
            return MavenModuleSet.class.equals(itemClass) ||
                    PluginsUtils.PROMOTION_BUILD_PLUGIN_CLASS.equals(itemClass.getSimpleName());
        }

        @JavaScriptMethod
        public RefreshServerResponse refreshResolversFromArtifactory(String url, String credentialsId, String username, String password, boolean overrideCredentials) {
            return super.refreshResolversFromArtifactory(url, credentialsId, username, password, overrideCredentials);
        }

        @SuppressWarnings("unused")
        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project) {
            return PluginsUtils.fillPluginCredentials(project);
        }

        @Override
        public String getHelpFile() {
            return "/plugin/artifactory/help/ArtifactoryMaven3NativeConfigurator/help.html";
        }
    }

    /**
     * Page Converter
     */
    public static final class ConverterImpl extends DeployerResolverOverriderConverter {
        public ConverterImpl(XStream2 xstream) {
            super(xstream);
        }
    }
}
