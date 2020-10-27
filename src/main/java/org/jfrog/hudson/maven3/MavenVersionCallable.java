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

package org.jfrog.hudson.maven3;

import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenEmbedderUtils;
import jenkins.security.MasterToSlaveCallable;

import java.io.File;
import java.io.IOException;

/**
 * Callable class that works on both master and slave, checks if the maven version is at least the required one that
 * is given in the constructor.
 *
 * @author Tomer Cohen
 */
public class MavenVersionCallable extends MasterToSlaveCallable<Boolean, IOException> {

    private final String mavenHome;
    private final String version;

    public MavenVersionCallable(String mavenHome, String version) {
        this.mavenHome = mavenHome;
        this.version = version;
    }

    public Boolean call() throws IOException {
        try {
            return MavenEmbedderUtils.isAtLeastMavenVersion(new File(mavenHome), version);
        } catch (MavenEmbedderException e) {
            throw new IOException(e);
        }
    }
}
