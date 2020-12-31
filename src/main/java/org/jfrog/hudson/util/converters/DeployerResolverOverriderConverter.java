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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.hudson.*;
import org.jfrog.hudson.gradle.ArtifactoryGradleConfigurator;
import org.jfrog.hudson.ivy.ArtifactoryIvyConfigurator;
import org.jfrog.hudson.ivy.ArtifactoryIvyFreeStyleConfigurator;
import org.jfrog.hudson.maven3.ArtifactoryMaven3NativeConfigurator;
import org.jfrog.hudson.util.Credentials;

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
@SuppressWarnings("deprecation")
public class DeployerResolverOverriderConverter<T> extends XStream2.PassthruConverter<T> {
    Logger logger = Logger.getLogger(DeployerResolverOverriderConverter.class.getName());
    List<String> converterErrors = Lists.newArrayList();
    // List of configurators that contain the useMavenPatterns parameter to be overridden
    List<String> useMavenPatternsOverrideList = Arrays.asList(ArtifactoryGradleConfigurator.class.getSimpleName(),
            ArtifactoryIvyConfigurator.class.getSimpleName(), ArtifactoryIvyFreeStyleConfigurator.class.getSimpleName());

    public DeployerResolverOverriderConverter(XStream2 xstream) {
        super(xstream);
    }

    @Override
    public void callback(T overrider, UnmarshallingContext context) {
        Class<?> overriderClass = overrider.getClass();
        if (overrider instanceof ResolverOverrider) {
            overrideResolverDetails((ResolverOverrider) overrider, overriderClass);
        }
        credentialsMigration(overrider, overriderClass);
        // Override after name change:
        if (overrider instanceof DeployerOverrider) {
            overrideDeployerDetails((DeployerOverrider) overrider, overriderClass);
        }
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
    public void credentialsMigration(T overrider, Class<?> overriderClass) {
        try {
            if (overrider instanceof DeployerOverrider) {
                deployerMigration(overrider, overriderClass);
            }
            if (overrider instanceof ResolverOverrider) {
                resolverMigration(overrider, overriderClass);
            }
        } catch (ReflectiveOperationException e) {
            converterErrors.add(getConversionErrorMessage(overrider, e));
        }
    }

    private void deployerMigration(T overrider, Class<?> overriderClass) throws ReflectiveOperationException {
        Field overridingDeployerCredentialsField = overriderClass.getDeclaredField("overridingDeployerCredentials");
        overridingDeployerCredentialsField.setAccessible(true);
        Object overridingDeployerCredentialsObj = overridingDeployerCredentialsField.get(overrider);

        Field deployerCredentialsConfigField = overriderClass.getDeclaredField("deployerCredentialsConfig");
        deployerCredentialsConfigField.setAccessible(true);

        if (overridingDeployerCredentialsObj != null) {
            Credentials overridingDeployerCredentials = (Credentials) overridingDeployerCredentialsObj;
            boolean shouldOverride = ((DeployerOverrider) overrider).getOverridingDeployerCredentials() != null;
            deployerCredentialsConfigField.set(overrider, new CredentialsConfig(overridingDeployerCredentials.getUsername(),
                    overridingDeployerCredentials.getPassword(), StringUtils.EMPTY, shouldOverride));
        } else {
            if (deployerCredentialsConfigField.get(overrider) == null) {
                deployerCredentialsConfigField.set(overrider, CredentialsConfig.EMPTY_CREDENTIALS_CONFIG);
            }
        }
    }

    private void resolverMigration(T overrider, Class<?> overriderClass) throws ReflectiveOperationException {
        Field resolverCredentialsField = overriderClass.getDeclaredField("overridingResolverCredentials");
        resolverCredentialsField.setAccessible(true);
        Object resolverCredentialsObj = resolverCredentialsField.get(overrider);

        Field resolverCredentialsConfigField = overriderClass.getDeclaredField("resolverCredentialsConfig");
        resolverCredentialsConfigField.setAccessible(true);
        if (resolverCredentialsObj != null) {
            Credentials resolverCredentials = (Credentials) resolverCredentialsObj;
            boolean shouldOverride = ((ResolverOverrider) overrider).getOverridingResolverCredentials() != null;
            CredentialsConfig credentialsConfig = new CredentialsConfig(resolverCredentials.getUsername(),
                    resolverCredentials.getPassword(), StringUtils.EMPTY, shouldOverride);
            resolverCredentialsConfigField.set(overrider, credentialsConfig);
        } else {
            if (resolverCredentialsConfigField.get(overrider) == null) {
                resolverCredentialsConfigField.set(overrider, CredentialsConfig.EMPTY_CREDENTIALS_CONFIG);
            }
        }
    }

    /**
     * Convert: details -> resolverDetails
     * Convert the "details" to "resolverDetails" if it doesn't exists already
     * In ArtifactoryMaven3NativeConfigurator the conversion is part of a name change only.
     */
    void overrideResolverDetails(ResolverOverrider overrider, Class<?> overriderClass) {
        try {
            Field oldDetailsField = overriderClass.getDeclaredField("details");
            oldDetailsField.setAccessible(true);
            Object oldResolverDetails = oldDetailsField.get(overrider);

            if (oldResolverDetails == null || oldDetailsField.get(overrider) == null) {
                return;
            }

            Field resolverDetailsField = overriderClass.getDeclaredField("resolverDetails");
            resolverDetailsField.setAccessible(true);
            if (resolverDetailsField.get(overrider) == null) {
                ServerDetails resolverServerDetails = createInitialResolveDetailsFromDeployDetails((ServerDetails) oldResolverDetails);
                resolverDetailsField.set(overrider, resolverServerDetails);
            }

        } catch (ReflectiveOperationException e) {
            converterErrors.add(getConversionErrorMessage(overrider, e));
        }
    }

    /**
     * Convert details to deployerDetails if it doesn't exists already
     * This conversion comes after a name change (details -> deployerDetails)
     */
    void overrideDeployerDetails(DeployerOverrider overrider, Class<?> overriderClass) {
        try {
            Field oldDetailsField = overriderClass.getDeclaredField("details");
            oldDetailsField.setAccessible(true);
            Object oldDeployerDetails = oldDetailsField.get(overrider);

            if (oldDeployerDetails == null || oldDetailsField.get(overrider) == null) {
                return;
            }

            Field deployerDetailsField = overriderClass.getDeclaredField("deployerDetails");
            deployerDetailsField.setAccessible(true);
            if (deployerDetailsField.get(overrider) == null) {
                ServerDetails deployerServerDetails = createInitialDeployDetailsFromOldDeployDetails((ServerDetails) oldDeployerDetails);
                deployerDetailsField.set(overrider, deployerServerDetails);
            }

        } catch (ReflectiveOperationException e) {
            converterErrors.add(getConversionErrorMessage(overrider, e));
        }
    }

    /**
     * Creates a new ServerDetails object for resolver, this will take URL and name from the deployer ServerDetails as a default behaviour
     */
    ServerDetails createInitialResolveDetailsFromDeployDetails(ServerDetails deployerDetails) {
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
    ServerDetails createInitialDeployDetailsFromOldDeployDetails(ServerDetails oldDeployerDetails) {
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
    private void overrideDeploymentProperties(T overrider, Class<?> overriderClass) {
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
            } catch (ReflectiveOperationException e) {
                converterErrors.add(getConversionErrorMessage(overrider, e));
            }
        }
    }

    /**
     * Convert the Boolean notM2Compatible parameter to Boolean useMavenPatterns (after applying !)
     * This conversion comes after a name change (!notM2Compatible -> useMavenPatterns)
     */
    private void overrideUseMavenPatterns(T overrider, Class<?> overriderClass) {
        if (useMavenPatternsOverrideList.contains(overriderClass.getSimpleName())) {
            try {
                Field useMavenPatternsField = overriderClass.getDeclaredField("useMavenPatterns");
                useMavenPatternsField.setAccessible(true);
                Object useMavenPatterns = useMavenPatternsField.get(overrider);

                if (useMavenPatterns == null) {
                    Field notM2CompatibleField = overriderClass.getDeclaredField("notM2Compatible");
                    notM2CompatibleField.setAccessible(true);
                    Object notM2Compatible = notM2CompatibleField.get(overrider);
                    if (notM2Compatible instanceof Boolean) {
                        useMavenPatternsField.set(overrider, !(Boolean) notM2Compatible);
                    }
                }
            } catch (ReflectiveOperationException e) {
                converterErrors.add(getConversionErrorMessage(overrider, e));
            }
        }
    }

    /**
     * Convert the Boolean skipInjectInitScript parameter to Boolean useArtifactoryGradlePlugin
     * This conversion comes after a name change (skipInjectInitScript -> useArtifactoryGradlePlugin)
     */
    private void overrideUseArtifactoryGradlePlugin(T overrider, Class<?> overriderClass) {
        if (overriderClass.getSimpleName().equals(ArtifactoryGradleConfigurator.class.getSimpleName())) {
            try {
                Field useArtifactoryGradlePluginField = overriderClass.getDeclaredField("useArtifactoryGradlePlugin");
                useArtifactoryGradlePluginField.setAccessible(true);
                Object useArtifactoryGradlePlugin = useArtifactoryGradlePluginField.get(overrider);

                if (useArtifactoryGradlePlugin == null) {
                    Field skipInjectInitScriptField = overriderClass.getDeclaredField("skipInjectInitScript");
                    skipInjectInitScriptField.setAccessible(true);
                    Object skipInjectInitScript = skipInjectInitScriptField.get(overrider);
                    if (skipInjectInitScript instanceof Boolean) {
                        useArtifactoryGradlePluginField.set(overrider, skipInjectInitScript);
                    }
                }
            } catch (ReflectiveOperationException e) {
                converterErrors.add(getConversionErrorMessage(overrider, e));
            }
        }
    }

    String getConversionErrorMessage(Object overrider, Exception e) {
        return String.format("Could not convert the class '%s' to use the new overriding" +
                "format. Cause: %s", overrider.getClass().getName(), ExceptionUtils.getRootCause(e));
    }
}
