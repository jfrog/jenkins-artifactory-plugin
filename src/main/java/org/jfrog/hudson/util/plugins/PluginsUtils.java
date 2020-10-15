package org.jfrog.hudson.util.plugins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.*;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.util.Credentials;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class PluginsUtils {
    public static final String MULTIJOB_PLUGIN_ID = "jenkins-multijob-plugin";
    public static final String GIT_PLUGIN_ID = "git";
    public static final String PROMOTION_BUILD_PLUGIN_CLASS = "PromotionProcess";
    public static final String JIRA_REST_SERVERINFO_ENDPOINT = "rest/api/2/serverInfo";

    private static ObjectMapper mapper;

    public static ListBoxModel fillPluginCredentials(Item project) {
        if (project == null || !project.hasPermission(Item.CONFIGURE)) {
            return new StandardListBoxModel();
        }
        return fillPluginCredentials(project, ACL.SYSTEM);
    }

    public static ListBoxModel fillPluginCredentials(Item project, Authentication authentication) {
        List<DomainRequirement> domainRequirements = Collections.emptyList();
        return new StandardListBoxModel()
                .withEmptySelection()
                .withMatching(
                        CredentialsMatchers.anyOf(
                                CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
                                CredentialsMatchers.instanceOf(StringCredentials.class),
                                CredentialsMatchers.instanceOf(StandardCertificateCredentials.class)
                        ),
                        CredentialsProvider.lookupCredentials(StandardCredentials.class,
                                project,
                                authentication,
                                domainRequirements)
                );
    }

    public static Credentials usernamePasswordCredentialsLookup(String credentialsId, Item item) {
        UsernamePasswordCredentials usernamePasswordCredentials = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        UsernamePasswordCredentials.class,
                        item,
                        item instanceof Queue.Task
                            ? Tasks.getAuthenticationOf((Queue.Task) item)
                            : ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(credentialsId)
        );

        if (usernamePasswordCredentials != null) {
            return new Credentials(usernamePasswordCredentials.getUsername(),
                    usernamePasswordCredentials.getPassword().getPlainText());
        }
        return Credentials.EMPTY_CREDENTIALS;
    }

    public static StringCredentials accessTokenCredentialsLookup(String credentialsId, Item item) {
        StringCredentials accessTokenPasswordCredentials = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StringCredentials.class,
                        item,
                        item instanceof Queue.Task
                                ? Tasks.getAuthenticationOf((Queue.Task) item)
                                : ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(credentialsId)
        );

        return accessTokenPasswordCredentials;
    }

    public static boolean isUseCredentialsPlugin() {
        return getDescriptor().getUseCredentialsPlugin();
    }

    private static ArtifactoryBuilder.DescriptorImpl getDescriptor() {
        ArtifactoryBuilder.DescriptorImpl descriptor = (ArtifactoryBuilder.DescriptorImpl)
                Hudson.getInstance().getDescriptor(ArtifactoryBuilder.class);
        if (descriptor != null) {
            return descriptor;
        }
        throw new IllegalStateException("ArtifactoryBuilder descriptor is null");
    }

    /**
     * If Credentials Plugin is enabled. Retrieves this value from the ArtifactoryBuilder class.
     *
     * @return Is Credentials Plugin enabled(true) or disabled(false)
     * @throws IllegalStateException
     */

    public static boolean isCredentialsPluginEnabled() {
        return getDescriptor().getUseCredentialsPlugin();
    }

    /**
     * From Jira plugin version 2.0 (and Jira 7.0) we are not able to retrieve the server info directly from
     * the Java API so we need to access the entry point directly
     *
     * @param jiraBaseUrl is the Jira URL as it was configured in the Jenkins global configuration
     * @return Jira version
     * @throws IOException
     */
    public static String getJiraVersion(URL jiraBaseUrl) {
        HttpClient client = new DefaultHttpClient();
        try {
            URL requestUrl = new URL(jiraBaseUrl + JIRA_REST_SERVERINFO_ENDPOINT);
            HttpResponse response = client.execute(new HttpGet(requestUrl.toURI()));
            lazyInitMapper();
            Map<String, Object> responseMap = mapper.readValue(response.getEntity().getContent(), new TypeReference<Map<String, Object>>() {
            });
            return ((String) responseMap.get("version"));
        } catch (Exception e) {
            throw new RuntimeException("Error while trying to get Jira Issue Tracker version: " + e.getMessage());
        }
    }

    private static ObjectMapper lazyInitMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
        }
        return mapper;
    }
}
