package org.jfrog.hudson.BintrayPublish;

import com.google.common.collect.Lists;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.release.BintrayUploadInfoOverride;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.client.bintrayResponse.BintrayResponse;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.util.VersionException;
import org.jfrog.hudson.*;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * This action is added to a successful build in order to push built artifacts to Bintray
 *
 * @author Aviad Shikloshi
 */
public class BintrayPublishAction<C extends BuildInfoAwareConfigurator & DeployerOverrider> extends TaskAction implements BuildBadgeAction {
    private final static String MINIMAL_SUPPORTED_VERSION = "3.5.3";
    private final AbstractBuild build;
    private final C configurator;
    private boolean override;
    private String subject;
    private String repoName;
    private String packageName;
    private String versionName;
    private String signMethod;
    private List<String> licenses;
    private String passphrase;
    private String vcsUrl;

    public BintrayPublishAction(AbstractBuild build, C configurator) {
        this.build = build;
        this.configurator = configurator;
    }

    public void doSubmit(StaplerRequest req, StaplerResponse resp) throws ServletException, IOException {
        getACL().checkPermission(getPermission());
        ArtifactoryServer artifactory = configurator.getArtifactoryServer();
        resetFields();
        req.bindParameters(this);
        if (!override){
            resetOverrideFields();
        }
        CredentialsConfig credentialsConfig = CredentialManager.getPreferredDeployer(configurator, configurator.getArtifactoryServer());
        new PushToBintrayWorker(artifactory, credentialsConfig.getCredentials(build.getProject())).start();
        resp.sendRedirect(".");
    }

    private void resetFields(){
        resetOverrideFields();
        resetQueryFields();
        this.override = false;
    }

    private void resetOverrideFields(){
        this.subject = null;
        this.repoName = null;
        this.packageName = null;
        this.versionName = null;
        this.licenses = Lists.newArrayList();
        this.vcsUrl = null;

    }
    private void resetQueryFields() {
        this.signMethod = null;
        this.passphrase = null;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void doIndex(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        req.getView(this, getCurrentAction()).forward(req, resp);
    }

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

    public boolean hasPushToBintrayPermission() {
         return getACL().hasPermission(getPermission()) && PluginsUtils.isPushToBintrayEnabled();
    }

    public synchronized String getCurrentAction() {
        return workerThread == null ? "form.jelly" : "progress.jelly";
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        if (StringUtils.isNotBlank(subject)) {
            this.subject = subject.trim();
        }
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        if (StringUtils.isNotEmpty(repoName)) {
            this.repoName = repoName.trim();
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        if (StringUtils.isNotEmpty(packageName)) {
            this.packageName = packageName;
        }
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        if (StringUtils.isNotEmpty(versionName)) {
            this.versionName = versionName;
        }
    }

    public String getSignMethod() {
        return signMethod;
    }

    public void setSignMethod(String signMethod) {
        if (StringUtils.isNotEmpty(signMethod)) {
            this.signMethod = signMethod;
        }
    }

    public String getLicenses() {
        return StringUtils.join(licenses, ",");
    }

    public void setLicenses(String licenses) {
        if (StringUtils.isNotBlank(licenses)) {
            this.licenses = Lists.newArrayList();
            String[] licenseList = licenses.split(",");
            for (String s : licenseList) {
                s = s.trim();
                if (!s.isEmpty()) {
                    this.licenses.add(s);
                }
            }
        } else {
            this.licenses = null;
        }
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        if (StringUtils.isNotEmpty(passphrase)) {
            this.passphrase = passphrase.trim();
        }
    }

    public String getVcsUrl() {
        return vcsUrl;
    }

    public void setVcsUrl(String vcsUrl) {
        if (StringUtils.isNotEmpty(vcsUrl)) {
            this.vcsUrl = vcsUrl.trim();
        }
    }

    public AbstractBuild getBuild() {
        return build;
    }

    @Override
    protected Permission getPermission() {
        return ArtifactoryPlugin.PUSH_TO_BINTRAY;
    }

    @Override
    protected ACL getACL() {
        return build.getACL();
    }

    public String getIconFileName() {
        return "/plugin/artifactory/images/bintray.png";
    }

    public String getDisplayName() {
        return "Push to Bintray";
    }

    public String getUrlName() {
        if (hasPushToBintrayPermission())
            return "bintray";
        else
            return null;
    }

    // Check of the current Artifactory version supports "Push to Bintray" API
    private boolean isValidArtifactoryVersion(ArtifactoryBuildInfoClient client) throws VersionException {
        ArtifactoryVersion version = client.verifyCompatibleArtifactoryVersion();
        return version.isAtLeast(new ArtifactoryVersion(MINIMAL_SUPPORTED_VERSION));
    }

    public final class PushToBintrayWorker extends TaskThread {

        private final ArtifactoryServer artifactoryServer;
        private final Credentials deployer;

        public PushToBintrayWorker(ArtifactoryServer artifactoryServer, Credentials deployer) {
            super(BintrayPublishAction.this, ListenerAndText.forMemory(null));
            this.artifactoryServer = artifactoryServer;
            this.deployer = deployer;
        }

        @Override
        protected void perform(TaskListener listener) throws IOException {
            long started = System.currentTimeMillis();

            BintrayUploadInfoOverride uploadInfoOverride =
                    new BintrayUploadInfoOverride(subject, repoName, packageName, versionName, licenses, vcsUrl);

            PrintStream logger = listener.getLogger();

            ArtifactoryBuildInfoClient client =
                    artifactoryServer.createArtifactoryClient(deployer.getUsername(), deployer.getPassword(),
                            artifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy));

            if (override && !uploadInfoOverride.isValid()) {
                logger.print("Please fill in all mandatory fields when pushing to Bintray without descriptor file\n");
            } else {
                try {
                    logger.println("Publishing to Bintray...");
                    if (isValidArtifactoryVersion(client)) {
                        String buildName = configurator.isOverrideBuildName() ? configurator.getCustomBuildName() : BuildUniqueIdentifierHelper.getBuildName(build);
                        String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
                        BintrayResponse response = client.pushToBintray(buildName, buildNumber, signMethod,
                                passphrase, uploadInfoOverride);
                        logger.println(response);
                    } else {
                        logger.println("Bintray push is not supported in your Artifactory version.");
                    }
                } catch (Exception e) {
                    logger.println(e.getMessage());
                }
            }
            // if the client gets back to the progress (after the redirect) page when this thread already done,
            // she will get an error message because the log dies with the thread. So lets delay up to 2 seconds
            long timeToWait = 2000 - (System.currentTimeMillis() - started);
            if (timeToWait > 0) {
                try {
                    Thread.sleep(timeToWait);
                } catch (InterruptedException ie) {
                    ie.printStackTrace(listener.error(ie.getMessage()));
                }
            }
            passphrase = null;
            workerThread = null;
            client.close();
        }
    }
}
