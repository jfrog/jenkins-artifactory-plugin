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

package org.jfrog.hudson.gradle;

import com.google.common.base.Charsets;
import hudson.EnvVars;
import hudson.FilePath;
import org.apache.commons.io.IOUtils;
import org.jfrog.hudson.util.PluginDependencyHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


/**
 * Class to generate a Gradle initialization script
 *
 * @author Tomer Cohen
 */
public class GradleInitScriptWriter {
    private FilePath rootPath;

    /**
     * The gradle initialization script constructor.
     */
    public GradleInitScriptWriter(FilePath rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Generate the init script from the Artifactory URL.
     *
     * @return The generated script.
     */
    public String generateInitScript(EnvVars env) throws IOException, InterruptedException {
        StringBuilder initScript = new StringBuilder();
        InputStream templateStream = getClass().getResourceAsStream("/initscripttemplate.gradle");
        String templateAsString = IOUtils.toString(templateStream, Charsets.UTF_8.name());
        File extractorJar = PluginDependencyHelper.getExtractorJar(env);
        FilePath dependencyDir = PluginDependencyHelper.getActualDependencyDirectory(extractorJar, rootPath);
        String absoluteDependencyDirPath = dependencyDir.getRemote();
        absoluteDependencyDirPath = absoluteDependencyDirPath.replace("\\", "/");
        String str = templateAsString.replace("${pluginLibDir}", absoluteDependencyDirPath);
        initScript.append(str);
        return initScript.toString();
    }
}