/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.hudson.release.maven;

import com.google.common.collect.Maps;
import hudson.maven.ModuleName;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Rewrites the project versions in the pom.
 *
 * @author Yossi Shaul
 */
public class PomTransformer extends MasterToSlaveFileCallable<Boolean> {

    private final String scmUrl;
    private final ModuleName currentModule;
    private final Map<ModuleName, String> versionsByModule;
    private final boolean failOnSnapshot;

    /**
     * Transforms single pom file.
     *
     * @param currentModule    The current module we work on
     * @param versionsByModule Map of module names to module version
     * @param scmUrl           Scm url to use if scm element exists in the pom file
     * @param failOnSnapshot   If true, fail with IllegalStateException if the pom contains snapshot version after the version changes
     */
    public PomTransformer(ModuleName currentModule, Map<ModuleName, String> versionsByModule, String scmUrl,
            boolean failOnSnapshot) {
        this.currentModule = currentModule;
        this.versionsByModule = versionsByModule;
        this.scmUrl = scmUrl;
        this.failOnSnapshot = failOnSnapshot;
    }

    /**
     * Performs the transformation.
     *
     * @return True if the file was modified.
     */
    public Boolean invoke(File pomFile, VirtualChannel channel) throws IOException, InterruptedException {

        org.jfrog.build.extractor.maven.reader.ModuleName current = new org.jfrog.build.extractor.maven.reader.ModuleName(
                currentModule.groupId, currentModule.artifactId);

        Map<org.jfrog.build.extractor.maven.reader.ModuleName, String> modules = Maps.newLinkedHashMap();
        for (Map.Entry<ModuleName, String> entry : versionsByModule.entrySet()) {
            modules.put(new org.jfrog.build.extractor.maven.reader.ModuleName(
                    entry.getKey().groupId, entry.getKey().artifactId), entry.getValue());
        }

        org.jfrog.build.extractor.maven.transformer.PomTransformer transformer =
                new org.jfrog.build.extractor.maven.transformer.PomTransformer(current, modules, scmUrl,
                        failOnSnapshot);

        return transformer.transform(pomFile);
    }
}
