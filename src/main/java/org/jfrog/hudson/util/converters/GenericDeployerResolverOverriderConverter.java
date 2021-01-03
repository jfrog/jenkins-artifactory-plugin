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

import hudson.util.XStream2;
import org.jfrog.hudson.DeployerOverrider;
import org.jfrog.hudson.ResolverOverrider;
import org.jfrog.hudson.ServerDetails;

import java.lang.reflect.Field;

/**
 * General converter for ArtifactoryGenericConfigurator
 *
 * @author yahavi
 */
public class GenericDeployerResolverOverriderConverter<T> extends DeployerResolverOverriderConverter<T> {

    public GenericDeployerResolverOverriderConverter(XStream2 xstream) {
        super(xstream);
    }

    /**
     * Convert: details -> resolverDetails -> specsResolverDetails/legacyResolverDetails
     * Convert the "resolverDetails" or "details" to "specsResolverDetails" or "legacyResolverDetails" if they doesn't exists already
     */
    @Override
    void overrideResolverDetails(ResolverOverrider overrider, Class<?> overriderClass) {
        try {
            Field oldResolverDetailsField = overriderClass.getDeclaredField("resolverDetails");
            oldResolverDetailsField.setAccessible(true);
            Object oldDeployerDetails = oldResolverDetailsField.get(overrider);
            if (oldDeployerDetails == null) {
                oldResolverDetailsField = overriderClass.getDeclaredField("details");
                oldResolverDetailsField.setAccessible(true);
                oldDeployerDetails = oldResolverDetailsField.get(overrider);
            }

            if (oldDeployerDetails == null || oldResolverDetailsField.get(overrider) == null) {
                // No old fields found, therefore convert is not needed
                return;
            }

            Field specsResolverDetailsField = overriderClass.getDeclaredField("specsResolverDetails");
            Field legacyResolverDetailsField = overriderClass.getDeclaredField("legacyResolverDetails");
            ServerDetails resolverServerDetails = createInitialResolveDetailsFromDeployDetails((ServerDetails) oldDeployerDetails);
            setServerDetailsIfNew(specsResolverDetailsField, overrider, resolverServerDetails);
            setServerDetailsIfNew(legacyResolverDetailsField, overrider, resolverServerDetails);
        } catch (ReflectiveOperationException e) {
            converterErrors.add(getConversionErrorMessage(overrider, e));
        }
    }

    /**
     * Convert deployerDetails -> details -> specsDeployerDetails/legacyDeployerDetails
     * Convert the details or deployerDetails to specsDeployerDetails or legacyDeployerDetails if they doesn't exists already
     * This conversion comes after a name change (deployerDetails -> specsDeployerDetails and legacyDeployerDetails)
     */
    @Override
    void overrideDeployerDetails(DeployerOverrider overrider, Class<?> overriderClass) {
        try {
            Field oldDeployerDetailsField = overriderClass.getDeclaredField("deployerDetails");
            oldDeployerDetailsField.setAccessible(true);
            Object oldDeployerDetails = oldDeployerDetailsField.get(overrider);
            if (oldDeployerDetails == null) {
                oldDeployerDetailsField = overrider.getClass().getDeclaredField("details");
                oldDeployerDetailsField.setAccessible(true);
                oldDeployerDetails = oldDeployerDetailsField.get(overrider);
            }

            if (oldDeployerDetails == null || oldDeployerDetailsField.get(overrider) == null) {
                // No old fields found, therefore convert is not needed
                return;
            }

            Field specsDeployerDetailsField = overriderClass.getDeclaredField("specsDeployerDetails");
            Field legacyDeployerDetailsField = overriderClass.getDeclaredField("legacyDeployerDetails");
            ServerDetails deployerServerDetails = createInitialDeployDetailsFromOldDeployDetails((ServerDetails) oldDeployerDetails);
            setServerDetailsIfNew(specsDeployerDetailsField, overrider, deployerServerDetails);
            setServerDetailsIfNew(legacyDeployerDetailsField, overrider, deployerServerDetails);
        } catch (ReflectiveOperationException e) {
            converterErrors.add(getConversionErrorMessage(overrider, e));
        }
    }

    /**
     * Set the server details, if they don't exist already.
     *
     * @param serverDetailsField - The field to set
     * @param overrider          - The server overrider
     * @param serverDetails      - The server details to set
     * @throws IllegalAccessException if the field is inaccessible
     */
    private void setServerDetailsIfNew(Field serverDetailsField, Object overrider, Object serverDetails) throws IllegalAccessException {
        serverDetailsField.setAccessible(true);
        if (serverDetailsField.get(overrider) == null) {
            serverDetailsField.set(overrider, serverDetails);
        }
    }
}
