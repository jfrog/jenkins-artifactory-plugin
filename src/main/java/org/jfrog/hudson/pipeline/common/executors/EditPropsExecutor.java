package org.jfrog.hudson.pipeline.common.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.generic.PropertiesEditorCallable;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.EditPropertiesHelper.EditPropertiesCommandType;

public class EditPropsExecutor {
    private final Run build;
    private transient FilePath ws;
    private ArtifactoryServer server;
    private TaskListener listener;
    private String spec;
    private EditPropertiesCommandType editType;
    private String props;
    private boolean failNoOp;

    public EditPropsExecutor(ArtifactoryServer server, TaskListener listener, Run build, FilePath ws, String spec,
                             EditPropertiesCommandType editType, String props, boolean failNoOp) {
        this.build = build;
        this.server = server;
        this.listener = listener;
        this.props = props;
        this.editType = editType;
        this.ws = ws;
        this.spec = spec;
        this.failNoOp = failNoOp;
    }

    public void execute() throws IOException, InterruptedException {
        CredentialsConfig preferredDeployer = server.getDeployerCredentialsConfig();
        Boolean success = ws.act(new PropertiesEditorCallable(new JenkinsBuildInfoLog(listener),
            preferredDeployer.provideUsername(build.getParent()),
            preferredDeployer.providePassword(build.getParent()),
            server.getUrl(), spec, Utils.getProxyConfiguration(server), editType, props));

        if (failNoOp && !success) {
            throw new RuntimeException("Fail-no-op: No files were affected in the edit properties process.");
        }
    }
}
