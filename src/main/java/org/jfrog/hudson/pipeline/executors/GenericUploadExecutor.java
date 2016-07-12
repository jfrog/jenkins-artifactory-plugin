package org.jfrog.hudson.pipeline.executors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.generic.GenericArtifactsDeployer;
import org.jfrog.hudson.pipeline.PipelineUtils;
import org.jfrog.hudson.pipeline.json.DownloadUploadJson;
import org.jfrog.hudson.pipeline.json.FileJson;
import org.jfrog.hudson.pipeline.types.BuildInfo;
import org.jfrog.hudson.pipeline.types.PipelineBuildInfoAccessor;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.ExtractorUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by romang on 4/24/16.
 */
public class GenericUploadExecutor {
    private transient FilePath ws;
    private transient Run build;
    private transient TaskListener listener;
    private BuildInfo buildinfo;
    private ArtifactoryServer server;
    private StepContext context;

    public GenericUploadExecutor(ArtifactoryServer server, TaskListener listener, Run build, FilePath ws, BuildInfo buildInfo, StepContext context) {
        this.server = server;
        this.listener = listener;
        this.build = build;
        this.buildinfo = PipelineUtils.prepareBuildinfo(build, buildInfo);
        this.ws = ws;
        this.context = context;
    }

    public BuildInfo execution(String json) throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        DownloadUploadJson uploadJson = mapper.readValue(json, DownloadUploadJson.class);
        uploadArtifacts(uploadJson);
        return buildinfo;
    }

    private void uploadArtifacts(DownloadUploadJson uploadJson) throws IOException, InterruptedException {
        ArtifactoryServer server = this.server;
        for (FileJson file : uploadJson.getFiles()) {
            ArrayListMultimap<String, String> propertiesToAdd = getPropertiesMap(file.getProps());
            Multimap<String, String> pairs = HashMultimap.create();

            String repoKey = getRepositoryKey(file.getTarget());
            pairs.put(file.getPattern(), getLocalPath(file.getTarget()));

            boolean isFlat = file.getFlat() == null || BooleanUtils.toBoolean(file.getFlat());
            boolean isRecursive = file.getRecursive() == null || BooleanUtils.toBoolean(file.getRecursive());
            boolean isRegexp = BooleanUtils.toBoolean(file.getRegexp());

            GenericArtifactsDeployer.FilesDeployerCallable deployer = new GenericArtifactsDeployer.FilesDeployerCallable(listener, pairs, server,
                    new Credentials(server.getResolvingCredentialsConfig().getUsername(), server.getResolvingCredentialsConfig().getPassword()),
                    repoKey, propertiesToAdd, server.createProxyConfiguration(Jenkins.getInstance().proxy));
            deployer.setPatternType(GenericArtifactsDeployer.FilesDeployerCallable.PatternType.WILDCARD);
            deployer.setRecursive(isRecursive);
            deployer.setFlat(isFlat);
            deployer.setRegexp(isRegexp);
            List<Artifact> artifactsToDeploy = ws.act(deployer);
            new PipelineBuildInfoAccessor(buildinfo).appendDeployedArtifacts(artifactsToDeploy);
        }
    }

    private ArrayListMultimap<String, String> getPropertiesMap(String props) throws IOException, InterruptedException {
        ArrayListMultimap<String, String> properties = ArrayListMultimap.create();

        if (buildinfo.getBuildName() != null) {
            properties.put("build.name", buildinfo.getBuildName());
        } else {
            properties.put("build.name", BuildUniqueIdentifierHelper.getBuildName(build));
        }
        if (buildinfo.getBuildNumber() != null) {
            properties.put("build.number", buildinfo.getBuildNumber());
        } else {
            properties.put("build.number", BuildUniqueIdentifierHelper.getBuildNumber(build));
        }
        properties.put("build.timestamp", build.getTimestamp().getTime().getTime() + "");
        Cause.UpstreamCause parent = ActionableHelper.getUpstreamCause(build);
        if (parent != null) {
            properties.put("build.parentName", ExtractorUtils.sanitizeBuildName(parent.getUpstreamProject()));
            properties.put("build.parentNumber", parent.getUpstreamBuild() + "");
        }
        EnvVars env = context.get(EnvVars.class);
        String revision = ExtractorUtils.getVcsRevision(env);
        if (StringUtils.isNotBlank(revision)) {
            properties.put(BuildInfoFields.VCS_REVISION, revision);
        }
        if (props == null) {
            return properties;
        }
        for (String prop : props.trim().split(";")) {
            String key = StringUtils.substringBefore(prop, "=");
            String values = StringUtils.substringAfter(prop, "=");
            for (String value : values.split(",")) {
                properties.put(key, value);
            }
        }
        return properties;
    }

    private String getRepositoryKey(String path) {
        return StringUtils.substringBefore(path, "/");
    }

    private String getLocalPath(String path) {
        return StringUtils.substringAfter(path, "/");
    }
}
