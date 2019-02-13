/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.hudson.release.gradle;

import hudson.AbortException;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.build.extractor.release.PropertiesTransformer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Rewrites the project properties in the {@code gradle.properties}.
 *
 * @author Tomer Cohen
 */
public class GradlePropertiesTransformer extends MasterToSlaveFileCallable<Boolean> {

    private final Map<String, String> versionsByName;

    public GradlePropertiesTransformer(Map<String, String> versionsByName) {
        this.versionsByName = versionsByName;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code True} in case the properties file was modified during the transformation. {@code false} otherwise
     */
    public Boolean invoke(File propertiesFile, VirtualChannel channel) throws IOException, InterruptedException {
        if (!propertiesFile.exists()) {
            throw new AbortException("Couldn't find properties file: " + propertiesFile.getAbsolutePath());
        }
        PropertiesTransformer transformer = new PropertiesTransformer(propertiesFile, versionsByName);
        return transformer.transform();
    }
}
