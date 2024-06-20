package org.jfrog.hudson;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.*;
import jenkins.model.Jenkins;
import jenkins.security.QueueItemAuthenticatorConfiguration;
import org.acegisecurity.Authentication;
import org.jfrog.hudson.util.Credentials;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockQueueItemAuthenticator;

import java.util.HashMap;
import java.util.Map;

public class CredentialsConfigTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testProvideCredentialsForRun() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        MockAuthorizationStrategy auth = new MockAuthorizationStrategy()
                .grant(Jenkins.READ).everywhere().to("alice", "bob")
                .grant(Computer.BUILD).everywhere().to("alice", "bob")
                // Item.CONFIGURE implies Credentials.USE_ITEM, which is what CredentialsProvider.findCredentialById
                // uses when determining whether to include item-scope credentials in the search.
                .grant(Item.CONFIGURE).everywhere().to("alice");
        j.jenkins.setAuthorizationStrategy(auth);

        String globalCredentialsId = "global-creds";
        IdCredentials globalCredentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
                globalCredentialsId, "test-global-creds", "global-user", "global-password");
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(Domain.global(), globalCredentials);

        FreeStyleProject p1 = j.createFreeStyleProject();
        FreeStyleProject p2 = j.createFreeStyleProject();

        Map<String, Authentication> jobsToAuths = new HashMap<>();
        jobsToAuths.put(p1.getFullName(), User.getById("alice", true).impersonate());
        jobsToAuths.put(p2.getFullName(), User.getById("bob", true).impersonate());
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().replace(new MockQueueItemAuthenticator(jobsToAuths));

        FreeStyleBuild r1 = j.buildAndAssertSuccess(p1);
        Credentials credentials1 = new CredentialsConfig("", "", globalCredentialsId).provideCredentials(r1);
        Assert.assertEquals("Alice has Credentials.USE_ITEM and should be able to use the credential", "global-user", credentials1.getUsername());
        Assert.assertEquals("global-password", credentials1.getPassword());

        FreeStyleBuild r2 = j.buildAndAssertSuccess(p2);
        Credentials credentials2 = new CredentialsConfig("", "", globalCredentialsId).provideCredentials(r2);
        Assert.assertEquals("Bob does not have Credentials.USE_ITEM and should not be able to use the credential", "", credentials2.getUsername());
        Assert.assertEquals("", credentials2.getPassword());
    }
}
