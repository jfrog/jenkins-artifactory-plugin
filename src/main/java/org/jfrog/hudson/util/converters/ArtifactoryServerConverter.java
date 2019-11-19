package org.jfrog.hudson.util.converters;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import hudson.util.XStream2;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.util.Credentials;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Lior Hasson
 */
public class ArtifactoryServerConverter extends XStream2.PassthruConverter<ArtifactoryServer> {
    Logger logger = Logger.getLogger(ArtifactoryServerConverter.class.getName());
    List<String> converterErrors = Lists.newArrayList();

    public ArtifactoryServerConverter(XStream2 xstream) {
        super(xstream);
    }

    @Override
    protected void callback(ArtifactoryServer server, UnmarshallingContext context) {
        credentialsMigration(server);

        if (!converterErrors.isEmpty()) {
            logger.info(converterErrors.toString());
        }
    }

    private void credentialsMigration(ArtifactoryServer server) {
        try {
            deployerMigration(server);
            resolverMigration(server);
        } catch (NoSuchFieldException e) {
            converterErrors.add(getConversionErrorMessage(server, e));
        } catch (IllegalAccessException e) {
            converterErrors.add(getConversionErrorMessage(server, e));
        } catch (IOException e) {
            converterErrors.add(getConversionErrorMessage(server, e));
        }
    }

    private void deployerMigration(ArtifactoryServer server) throws NoSuchFieldException, IllegalAccessException, IOException {
        Class<? extends ArtifactoryServer> overriderClass = server.getClass();
        Field deployerCredentialsField = overriderClass.getDeclaredField("deployerCredentials");
        deployerCredentialsField.setAccessible(true);
        Object deployerCredentialsObj = deployerCredentialsField.get(server);

        Field deployerCredentialsConfigField = overriderClass.getDeclaredField("deployerCredentialsConfig");
        deployerCredentialsConfigField.setAccessible(true);

        if (deployerCredentialsObj != null) {
            Credentials deployerCredentials = (Credentials) deployerCredentialsObj;
            deployerCredentialsConfigField.set(server, new CredentialsConfig(deployerCredentials.getUsername(),
                    deployerCredentials.getPassword(), StringUtils.EMPTY, true));
        } else {
            if (deployerCredentialsConfigField.get(server) == null) {
                deployerCredentialsConfigField.set(server, CredentialsConfig.EMPTY_CREDENTIALS_CONFIG);
            }
        }
    }

    private void resolverMigration(ArtifactoryServer server)
            throws NoSuchFieldException, IllegalAccessException, IOException {
        Class<? extends ArtifactoryServer> overriderClass = server.getClass();
        Field resolverCredentialsField = overriderClass.getDeclaredField("resolverCredentials");
        Field resolverCredentialsConfig = overriderClass.getDeclaredField("resolverCredentialsConfig");
        resolverCredentialsConfig.setAccessible(true);
        resolverCredentialsField.setAccessible(true);
        Object resolverCredentialsObj = resolverCredentialsField.get(server);

        if (resolverCredentialsObj != null) {
            Credentials resolverCredentials = (Credentials) resolverCredentialsObj;
            resolverCredentialsConfig.set(server, new CredentialsConfig(resolverCredentials.getUsername(), resolverCredentials.getPassword(), StringUtils.EMPTY, true));
        } else {
            if (resolverCredentialsConfig.get(server) == null) {
                resolverCredentialsConfig.set(server, CredentialsConfig.EMPTY_CREDENTIALS_CONFIG);
            }
        }
    }

    private String getConversionErrorMessage(ArtifactoryServer artifactoryServer, Exception e) {
        return String.format("Could not convert the class '%s' to use the new overriding" +
                "format. Cause: %s", artifactoryServer.getClass().getName(), e.getCause());
    }
}