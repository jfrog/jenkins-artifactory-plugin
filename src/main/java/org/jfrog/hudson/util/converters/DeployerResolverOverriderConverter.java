/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson.util.converters;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import hudson.util.XStream2;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.*;
import org.jfrog.hudson.gradle.ArtifactoryGradleConfigurator;
import org.jfrog.hudson.ivy.ArtifactoryIvyConfigurator;
import org.jfrog.hudson.ivy.ArtifactoryIvyFreeStyleConfigurator;
import org.jfrog.hudson.maven3.ArtifactoryMaven3NativeConfigurator;
import org.jfrog.hudson.util.Credentials;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * General converter for BuildWrapper or Publisher
 *
 * @author Noam Y. Tenne
 * @author Lior Hasson
 */
public class DeployerResolverOverriderConverter<T> extends XStream2.PassthruConverter<T> {
    Logger logger = Logger.getLogger(DeployerResolverOverriderConverter.class.getName());
    List<String> converterErrors = Lists.newArrayList();
    // List of configurators that contain the useMavenPatterns parameter to be overrided
    List<String> useMavenPatternsOverrideList = Arrays.asList(ArtifactoryGradleConfigurator.class.getSimpleName(),
            ArtifactoryIvyConfigurator.class.getSimpleName(), ArtifactoryIvyFreeStyleConfigurator.class.getSimpleName());

    public DeployerResolverOverriderConverter(XStream2 xstream) {
        super(xstream);
    }

    @Override
    protected void callback(T overrider, UnmarshallingContext context) {
        Class overriderClass = overrider.getClass();
        overrideResolverDetails(overrider, overriderClass);
        credentialsMigration(overrider, overriderClass);
        // Override after name change:
        overrideDeployerDetails(overrider, overriderClass);
        overrideDeploymentProperties(overrider, overriderClass);
        overrideUseMavenPatterns(overrider, overriderClass);
        overrideUseArtifactoryGradlePlugin(overrider, overriderClass);

        if (!converterErrors.isEmpty()) {
            logger.info(converterErrors.toString());
        }
    }

    /**
     * Migrate to Jenkins "Credentials" plugin from the old credential implementation
     */
    public void credentialsMigration(T overrider, Class overriderClass) {
        try {
            deployerMigration(overrider, overriderClass);
            resolverMigration(overrider, overriderClass);
        } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
            converterErrors.add(getConversionErrorMessage(overrider, e));
        }
    }

    private void deployerMigration(T overrider, Class overriderClass)
            throws NoSuchFieldException, IllegalAccessException, IOException {
        if (overrider instanceof DeployerOverrider) {
            Field overridingDeployerCredentialsField = overriderClass.getDeclaredField("overridingDeployerCredentials");
            overridingDeployerCredentialsField.setAccessible(true);
            Object overridingDeployerCredentials = overridingDeployerCredentialsField.get(overrider);

            Field deployerCredentialsConfigField = overriderClass.getDeclaredField("deployerCredentialsConfig");
            deployerCredentialsConfigField.setAccessible(true);

            if (overridingDeployerCredentials != null) {
                boolean shouldOverride = ((DeployerOverrider)overrider).getOverridingDeployerCredentials() != null;
                deployerCredentialsConfigField.set(overrider, new CredentialsConfig((Credentials) overridingDeployerCredentials,
                        StringUtils.EMPTY, shouldOverride));
            } else {
                if (deployerCredentialsConfigField.get(overrider) == null) {
                    deployerCredentialsConfigField.set(overrider, CredentialsConfig.EMPTY_CREDENTIALS_CONFIG);
                }
            }
        }
    }

    private void resolverMigration(T overrider, Class overriderClass)
            throws NoSuchFieldException, IllegalAccessException, IOException {
        if (overrider instanceof ResolverOverrider) {
            Field resolverCredentialsField = overriderClass.getDeclaredField("overridingResolverCredentials");
            resolverCredentialsField.setAccessible(true);
            Object resolverCredentials = resolverCredentialsField.get(overrider);

            Field resolverCredentialsConfigField = overriderClass.getDeclaredField("resolverCredentialsConfig");
            resolverCredentialsConfigField.setAccessible(true);
            if (resolverCredentials != null) {
                boolean shouldOverride = ((ResolverOverrider)overrider).getOverridingResolverCredentials() != null;
                CredentialsConfig credentialsConfig = new CredentialsConfig((Credentials) resolverCredentials, StringUtils.EMPTY, shouldOverride);
                resolverCredentialsConfigField.set(overrider, credentialsConfig);
            } else {
                if (resolverCredentialsConfigField.get(overrider) == null) {
                    resolverCredentialsConfigField.set(overrider, CredentialsConfig.EMPTY_CREDENTIALS_CONFIG);
                }
            }
        }
    }

    /**
     * Convert the (ServerDetails)details to (ServerDetails)resolverDetails if it doesn't exists already
     * This conversion comes after a separation to two ServerDetails (resolver and deployer).
     * Incase the configuration has only one ServerDetails instance called "details", create a new one for resolver.
     * In ArtifactoryMaven3NativeConfigurator the conversion is part of a name change only.
     */
    private void overrideResolverDetails(T overrider, Class overriderClass) {
        if (overrider instanceof ResolverOverrider) {
            try {
                Field resolverDetailsField = overriderClass.getDeclaredField("resolverDetails");
                resolverDetailsField.setAccessible(true);
                Object resolverDetails = resolverDetailsField.get(overrider);

                if (resolverDetails == null) {
                    Field oldDeployerDetailsField = overriderClass.getDeclaredField("details");
                    oldDeployerDetailsField.setAccessible(true);
                    Object oldDeployerDetails = oldDeployerDetailsField.get(overrider);
                    if (oldDeployerDetails != null) {
                        ServerDetails resolverServerDetails = createInitialResolveDetailsFromDeployDetails((ServerDetails) oldDeployerDetails);
                        resolverDetailsField.set(overrider, resolverServerDetails);
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                converterErrors.add(getConversionErrorMessage(overrider, e));
            }
        }
    }

    /**
     * Convert the (ServerDetails)details to (ServerDetails)deployerDetails if it doesn't exists already
     * This convertion comes after a name change (details -> deployerDetails)
     */
    private void overrideDeployerDetails(T overrider, Class overriderClass) {
        if (overrider instanceof DeployerOverrider) {
            try {
                Field deployerDetailsField = overriderClass.getDeclaredField("deployerDetails");
                deployerDetailsField.setAccessible(true);
                Object deployerDetails = deployerDetailsField.get(overrider);

                if (deployerDetails == null) {
                    Field oldDeployerDetailsField = overriderClass.getDeclaredField("details");
                    oldDeployerDetailsField.setAccessible(true);
                    Object oldDeployerDetails = oldDeployerDetailsField.get(overrider);
                    if (oldDeployerDetails != null) {
                        ServerDetails deployerServerDetails = createInitialDeployDetailsFromOldDeployDetails((ServerDetails) oldDeployerDetails);
                        deployerDetailsField.set(overrider, deployerServerDetails);
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                converterErrors.add(getConversionErrorMessage(overrider, e));
            }
        }
    }

    /**
     * Creates a new ServerDetails object for resolver, this will take URL and name from the deployer ServerDetails as a default behaviour
     */
    private ServerDetails createInitialResolveDetailsFromDeployDetails(ServerDetails deployerDetails) {
        RepositoryConf oldResolveRepositoryConfig = deployerDetails.getResolveReleaseRepository();
        RepositoryConf oldSnapshotResolveRepositoryConfig = deployerDetails.getResolveSnapshotRepository();
        RepositoryConf resolverReleaseRepos = oldResolveRepositoryConfig == null ? RepositoryConf.emptyRepositoryConfig : oldResolveRepositoryConfig;
        RepositoryConf resolveSnapshotRepos = oldSnapshotResolveRepositoryConfig == null ? RepositoryConf.emptyRepositoryConfig : oldSnapshotResolveRepositoryConfig;
        return new ServerDetails(deployerDetails.getArtifactoryName(), deployerDetails.getArtifactoryUrl(),
                null, null, resolverReleaseRepos, resolveSnapshotRepos, null, null);
    }

    /**
     * Creates a new ServerDetails object for deployer, this will take URL and name from the oldDeployer ServerDetails
     */
    private ServerDetails createInitialDeployDetailsFromOldDeployDetails(ServerDetails oldDeployerDetails) {
        RepositoryConf oldDeployRepositoryConfig = oldDeployerDetails.getDeployReleaseRepository();
        RepositoryConf oldSnapshotDeployRepositoryConfig = oldDeployerDetails.getDeploySnapshotRepository();
        RepositoryConf deployReleaseRepos = oldDeployRepositoryConfig == null ? RepositoryConf.emptyRepositoryConfig : oldDeployRepositoryConfig;
        RepositoryConf deploySnapshotRepos = oldSnapshotDeployRepositoryConfig == null ? RepositoryConf.emptyRepositoryConfig : oldSnapshotDeployRepositoryConfig;
        return new ServerDetails(oldDeployerDetails.getArtifactoryName(), oldDeployerDetails.getArtifactoryUrl(),
                deployReleaseRepos, deploySnapshotRepos, null, null, null, null);
    }

    /**
     * Convert the String matrixParams parameter to String deploymentProperties
     * This convertion comes after a name change (matrixParams -> deploymentProperties)
     */
    private void overrideDeploymentProperties(T overrider, Class overriderClass) {
        if (!overriderClass.getSimpleName().equals(ArtifactoryMaven3NativeConfigurator.class.getSimpleName())) {
            try {
                Field deploymentPropertiesField = overriderClass.getDeclaredField("deploymentProperties");
                deploymentPropertiesField.setAccessible(true);
                Object deploymentProperties = deploymentPropertiesField.get(overrider);

                if (deploymentProperties == null) {
                    Field matrixParamsField = overriderClass.getDeclaredField("matrixParams");
                    matrixParamsField.setAccessible(true);
                    Object matrixParams = matrixParamsField.get(overrider);
                    if (matrixParams != null) {
                        deploymentPropertiesField.set(overrider, matrixParams);
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                converterErrors.add(getConversionErrorMessage(overrider, e));
            }
        }
    }

    /**
     * Convert the Boolean notM2Compatible parameter to Boolean useMavenPatterns (after applying !)
     * This convertion comes after a name change (!notM2Compatible -> useMavenPatterns)
     */
    private void overrideUseMavenPatterns(T overrider, Class overriderClass) {
        if (useMavenPatternsOverrideList.contains(overriderClass.getSimpleName())) {
            try {
                Field useMavenPatternsField = overriderClass.getDeclaredField("useMavenPatterns");
                useMavenPatternsField.setAccessible(true);
                Object useMavenPatterns = useMavenPatternsField.get(overrider);

                if (useMavenPatterns == null) {
                    Field notM2CompatibleField = overriderClass.getDeclaredField("notM2Compatible");
                    notM2CompatibleField.setAccessible(true);
                    Object notM2Compatible = notM2CompatibleField.get(overrider);
                    if (notM2Compatible instanceof Boolean && notM2Compatible != null) {
                        useMavenPatternsField.set(overrider, !(Boolean)notM2Compatible);
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                converterErrors.add(getConversionErrorMessage(overrider, e));
            }
        }
    }

    /**
     * Convert the Boolean skipInjectInitScript parameter to Boolean useArtifactoryGradlePlugin
     * This convertion comes after a name change (skipInjectInitScript -> useArtifactoryGradlePlugin)
     */
    private void overrideUseArtifactoryGradlePlugin(T overrider, Class overriderClass) {
        if (overriderClass.getSimpleName().equals(ArtifactoryGradleConfigurator.class.getSimpleName())) {
            try {
                Field useArtifactoryGradlePluginField = overriderClass.getDeclaredField("useArtifactoryGradlePlugin");
                useArtifactoryGradlePluginField.setAccessible(true);
                Object useArtifactoryGradlePlugin = useArtifactoryGradlePluginField.get(overrider);

                if (useArtifactoryGradlePlugin == null) {
                    Field skipInjectInitScriptField = overriderClass.getDeclaredField("skipInjectInitScript");
                    skipInjectInitScriptField.setAccessible(true);
                    Object skipInjectInitScript = skipInjectInitScriptField.get(overrider);
                    if (skipInjectInitScript instanceof Boolean && skipInjectInitScript != null) {
                        useArtifactoryGradlePluginField.set(overrider, skipInjectInitScript);
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                converterErrors.add(getConversionErrorMessage(overrider, e));
            }
        }
    }

    private String getConversionErrorMessage(T deployerOverrider, Exception e) {
        return String.format("Could not convert the class '%s' to use the new overriding" +
                "format. Cause: %s", deployerOverrider.getClass().getName(), e.getCause());
    }
}
