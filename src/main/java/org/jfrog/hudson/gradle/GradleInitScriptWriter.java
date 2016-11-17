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
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.remoting.Which;
import org.apache.commons.io.IOUtils;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.util.PluginDependencyHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;


/**
 * Class to generate a Gradle initialization script
 *
 * @author Tomer Cohen
 */
public class GradleInitScriptWriter {
    private Run build;
    private Launcher launcher;

    /**
     * The gradle initialization script constructor.
     *
     * @param build
     */
    public GradleInitScriptWriter(Run build, Launcher launcher) {
        this.build = build;
        this.launcher = launcher;
    }

    /**
     * Generate the init script from the Artifactory URL.
     *
     * @return The generated script.
     */
    public String generateInitScript() throws URISyntaxException, IOException, InterruptedException {
        StringBuilder initScript = new StringBuilder();
        InputStream templateStream = getClass().getResourceAsStream("/initscripttemplate.gradle");
        String templateAsString = IOUtils.toString(templateStream, Charsets.UTF_8.name());
        File localGradleExtractorJar = Which.jarFile(getClass().getResource("/initscripttemplate.gradle"));
        FilePath dependencyDir = PluginDependencyHelper.getActualDependencyDirectory(localGradleExtractorJar, Utils.getNode(launcher).getRootPath());
        String absoluteDependencyDirPath = dependencyDir.getRemote();
        absoluteDependencyDirPath = absoluteDependencyDirPath.replace("\\", "/");
        String str = templateAsString.replace("${pluginLibDir}", absoluteDependencyDirPath);
        initScript.append(str);
        return initScript.toString();
    }
}