package org.jfrog.hudson.pipeline;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.git.util.BuildData;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.docker.proxy.CertManager;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.IncludesExcludes;
import org.jfrog.hudson.util.RepositoriesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by romang on 4/24/16.
 */
public class Utils {

    public static final String BUILD_INFO_DELIMITER = ".";

    /**
     * Prepares Artifactory server either from serverID or from ArtifactoryServer.
     *
     * @param artifactoryServerID
     * @param pipelineServer
     * @return
     */
    public static org.jfrog.hudson.ArtifactoryServer prepareArtifactoryServer(String artifactoryServerID,
                                                                              ArtifactoryServer pipelineServer) {

        if (artifactoryServerID == null && pipelineServer == null) {
            return null;
        }
        if (artifactoryServerID != null && pipelineServer != null) {
            return null;
        }
        if (pipelineServer != null) {
            CredentialsConfig credentials = pipelineServer.createCredentialsConfig();

            return new org.jfrog.hudson.ArtifactoryServer(null, pipelineServer.getUrl(), credentials,
                    credentials, 0, pipelineServer.isBypassProxy());
        }
        org.jfrog.hudson.ArtifactoryServer server = RepositoriesUtils.getArtifactoryServer(artifactoryServerID, RepositoriesUtils.getArtifactoryServers());
        if (server == null) {
            return null;
        }
        return server;
    }

    public static ListBoxModel getServerListBox() {
        ListBoxModel r = new ListBoxModel();
        List<org.jfrog.hudson.ArtifactoryServer> servers = RepositoriesUtils.getArtifactoryServers();
        r.add("", "");
        for (org.jfrog.hudson.ArtifactoryServer server : servers) {
            r.add(server.getName() + Utils.BUILD_INFO_DELIMITER + server.getUrl(), server.getName());
        }
        return r;
    }

    public static BuildInfo prepareBuildinfo(Run build, BuildInfo buildinfo) {
        if (buildinfo == null) {
            return new BuildInfo(build);
        }
        return buildinfo;
    }

    public static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static EnvVars extractBuildParameters(Run build, TaskListener listener) {
        EnvVars buildVariables = new EnvVars();
        try {
            ParametersAction parameters = build.getAction(ParametersAction.class);
            if (parameters != null) {
                for (ParameterValue p : parameters) {
                    if (p.isSensitive()) {
                        continue;
                    }
                    String v = p.createVariableResolver(null).resolve(p.getName());
                    if (v != null) {
                        buildVariables.put(p.getName(), v);
                    }
                }
            }
        } catch (Exception e) {
            listener.getLogger().println("Can't get build variables");
            return null;
        }

        return buildVariables;
    }

    public static List<Vcs> extractVcsBuildData(Run build) {
        List<Vcs> result = new ArrayList<Vcs>();
        List<BuildData> buildData = build.getActions(BuildData.class);
        if (buildData != null) {
            for (BuildData data : buildData) {
                String sha1 = data.getLastBuiltRevision().getSha1String();
                Iterator<String> iterator = data.getRemoteUrls().iterator();
                if (iterator.hasNext()) {
                    result.add(new Vcs(iterator.next(), sha1));
                }
            }
        }
        return result;
    }

    public static Node getNode(Launcher launcher) {
        Node node = null;
        Jenkins j = Jenkins.getInstance();
        for (Computer c : j.getComputers()) {
            if (c.getChannel() == launcher.getChannel()) {
                node = c.getNode();
                break;
            }
        }
        return node;
    }

    public static Computer getCurrentComputer(Launcher launcher) {
        Jenkins j = Jenkins.getInstance();
        for (Computer c : j.getComputers()) {
            if (c.getChannel() == launcher.getChannel()) {
                return c;
            }
        }
        return null;
    }

    public static IncludesExcludes getArtifactsIncludeExcludeForDeyployment(IncludeExcludePatterns patternFilter) {
        if (patternFilter == null) {
            return new IncludesExcludes("", "");
        }
        String[] excludePatterns = patternFilter.getExcludePatterns();
        String[] includePatterns = patternFilter.getIncludePatterns();
        StringBuilder include = new StringBuilder();
        StringBuilder exclude = new StringBuilder();
        for (int i = 0; i < includePatterns.length; i++) {
            if (i < includePatterns.length - 1 && include.length() > 0) {
                include.append(", ");
            }
            include.append(includePatterns[i]);
        }
        for (int i = 0; i < excludePatterns.length; i++) {
            if (i < excludePatterns.length - 1 && exclude.length() > 0) {
                exclude.append(", ");
            }
            exclude.append(excludePatterns[i]);
        }

        IncludesExcludes result = new IncludesExcludes(include.toString(), exclude.toString());
        return result;
    }

    public static org.jfrog.build.api.Build getGeneratedBuildInfo(Run build, EnvVars env, TaskListener listener, FilePath ws, Launcher launcher) {
        ObjectMapper mapper = new ObjectMapper();
        FilePath generatedBuildInfoFilePath = null;
        InputStream inputStream = null;
        try {
            StringWriter writer = new StringWriter();
            generatedBuildInfoFilePath = new FilePath(launcher.getChannel(), env.get(BuildInfoFields.GENERATED_BUILD_INFO));
            inputStream = generatedBuildInfoFilePath.read();
            IOUtils.copy(inputStream, writer, "UTF-8");
            String theString = writer.toString();
            return mapper.readValue(theString, org.jfrog.build.api.Build.class);
        } catch (Exception e) {
            listener.error("Couldn't read generated build info at : " + env.get(BuildInfoFields.GENERATED_BUILD_INFO));
            build.setResult(Result.FAILURE);
            throw new Run.RunnerAbortedException();
        } finally {
            if (inputStream != null) {
                IOUtils.closeQuietly(inputStream);
            }
            deleteFilePathQuietly(generatedBuildInfoFilePath);
        }
    }

    private static void deleteFilePathQuietly(FilePath  filePath) {
        try {
          if (filePath != null && filePath.exists()) {
              filePath.delete();
          }
        } catch (Exception e) {
            // Ignore exceptions
        }
    }

    public static void copyCertsToAgent(Computer c) throws IOException, InterruptedException {
        if (!(c instanceof Jenkins.MasterComputer)) {

            String certPath = CertManager.DEFAULT_RELATIVE_CERT_PATH;
            FilePath remotePublicKey = new FilePath(c.getChannel(), c.getNode().getRootPath() + "/" + certPath);
            FilePath localPublicKey = new FilePath(Jenkins.getInstance().getRootPath(), certPath);
            localPublicKey.copyTo(remotePublicKey);

            String keyPath = CertManager.DEFAULT_RELATIVE_KEY_PATH;
            FilePath remotePrivateKey = new FilePath(c.getChannel(), c.getNode().getRootPath() + "/" + keyPath);
            FilePath localPrivateKey = new FilePath(Jenkins.getInstance().getRootPath(), keyPath);
            localPrivateKey.copyTo(remotePrivateKey);
        }
    }

}
