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

package org.jfrog.hudson.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.release.gradle.GradleModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Utility class to help rewrite a properties file while keeping the contents of the properties file intact (e.g
 * comments)
 *
 * @author Tomer Cohen
 */
public class PropertyUtils {
    private static final Logger debuggingLogger = Logger.getLogger(PropertyUtils.class.getName());

    private PropertyUtils() {
        // utility class
        throw new IllegalAccessError();
    }

    /**
     * Read the {@code gradle.properties} and parse it into {@link GradleModule}s. Only those keys which are given will
     * be included in the parsing.
     *
     * @param gradlePropPath The path of the gradle.properties file
     * @param propKeys       The property keys that should be taken into account when reading the properties file.
     * @return A map of {@link GradleModule}s that were assembled from the properties file, with the version as its
     *         value.
     * @throws IOException In case an error occurs while reading the properties file, this exception is thrown.
     */
    public static Map<String, String> getModulesPropertiesFromPropFile(FilePath gradlePropPath, String[] propKeys)
            throws IOException, InterruptedException {
        Properties gradleProps = loadGradleProperties(gradlePropPath);
        Map<String, String> versionsByPropKey = Maps.newLinkedHashMap();
        for (String propKey : propKeys) {
            if (gradleProps.containsKey(propKey)) {
                versionsByPropKey.put(propKey, gradleProps.getProperty(propKey));
            }
        }
        return versionsByPropKey;
    }

    /**
     * Load a properties file from a file path
     *
     * @param gradlePropertiesFilePath The file path where the gradle.properties is located.
     * @return The loaded properties.
     * @throws IOException In case an error occurs while reading the properties file, this exception is thrown.
     */
    private static Properties loadGradleProperties(FilePath gradlePropertiesFilePath)
            throws IOException, InterruptedException {
        return gradlePropertiesFilePath.act(new MasterToSlaveFileCallable<Properties>() {
            public Properties invoke(File gradlePropertiesFile, VirtualChannel channel) throws IOException, InterruptedException {
                Properties gradleProps = new Properties();
                if (gradlePropertiesFile.exists()) {
                    debuggingLogger.fine("Gradle properties file exists at: " + gradlePropertiesFile.getAbsolutePath());
                    FileInputStream stream = null;
                    try {
                        stream = new FileInputStream(gradlePropertiesFile);
                        gradleProps.load(stream);
                    } catch (IOException e) {
                        debuggingLogger.fine("IO exception occurred while trying to read properties file from: " +
                                gradlePropertiesFile.getAbsolutePath());
                        throw new RuntimeException(e);
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                }
                return gradleProps;
            }
        });

    }

    public static Multimap<String, String> getDeploymentPropertiesMap(String propertiesStr, EnvVars env) {
        Multimap<String, String> properties = ArrayListMultimap.create();
        String[] deploymentProperties = StringUtils.split(propertiesStr, ";");
        if (deploymentProperties == null) {
            return properties;
        }
        for (String property : deploymentProperties) {
            String[] split = StringUtils.split(property, '=');
            if (split.length == 2) {
                String value = Util.replaceMacro(split[1], env);
                //Space is not allowed in property key
                properties.put(split[0].replace(" ", StringUtils.EMPTY), value);
            }
        }
        return properties;
    }
}
