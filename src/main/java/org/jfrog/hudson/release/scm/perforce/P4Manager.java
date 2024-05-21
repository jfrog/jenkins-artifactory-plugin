package org.jfrog.hudson.release.scm.perforce;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IOptionsServer;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.scm.SCM;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.p4.PerforceScm;
import org.jenkinsci.plugins.p4.client.ClientHelper;
import org.jenkinsci.plugins.p4.client.ConnectionFactory;
import org.jenkinsci.plugins.p4.client.ConnectionHelper;
import org.jenkinsci.plugins.p4.workspace.Workspace;
import org.jfrog.build.vcs.perforce.PerforceClient;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interacts with Perforce various release operations.
 * Using the updated perforce plugin - P4.
 *
 * @author Aviad Shikloshi
 */
public class P4Manager extends AbstractPerforceManager<PerforceScm> {

    private static final Logger logger = Logger.getLogger(P4Manager.class.getName());

    public P4Manager(AbstractBuild<?, ?> build, TaskListener buildListener) {
        super(build, buildListener);
    }

    @Override
    public void prepare() {

        PerforceScm perforceScm = getJenkinsScm();
        String credentials = perforceScm.getCredential();

        try {
            ConnectionHelper connection = new ConnectionHelper(build, credentials, buildListener);
            IOptionsServer server = ConnectionFactory.getConnection();
            Workspace clientString = getClientWorkspace();
            if (connection.isClient(clientString.getName())) {
                ClientHelper perforceClient = new ClientHelper((Item)this.build.getProject(), credentials, buildListener, clientString);
                IClient client = perforceClient.getClient();
                try {
                    this.perforce = new PerforceClient(server, client);
                    this.perforce.initConnection();
                } catch (Exception e) {
                    logger.warning("Could not instantiate connection with PerforceClient: " + e.getMessage());
                }
            } else {
                logger.warning("Client " + clientString + " is not a valid client.");
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Error occurred: ", e);
        }

    }

    @Override
    public PerforceClient establishConnection() throws Exception {
        this.perforce.initConnection();
        return this.perforce;
    }

    private Workspace getClientWorkspace() {
        SCM scm = this.build.getProject().getScm();
        if (scm instanceof PerforceScm) {
            PerforceScm p4scm = (PerforceScm)scm;
            return p4scm.getWorkspace();
        }
        logger.log(Level.FINE, "Unable to determine P4 workspace");
        return null;
    }
}
