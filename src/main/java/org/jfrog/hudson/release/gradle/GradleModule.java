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

import java.io.Serializable;

/**
 * Simple data object to store the Gradle module's name and its assigned version.
 *
 * @author Tomer Cohen
 */
public class GradleModule implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String moduleName;
    private final String moduleVersion;

    public GradleModule(String moduleName, String moduleVersion) {
        this.moduleName = moduleName;
        this.moduleVersion = moduleVersion;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getModuleVersion() {
        return moduleVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GradleModule module = (GradleModule) o;

        return moduleName.equals(module.moduleName);
    }

    @Override
    public int hashCode() {
        return moduleName.hashCode();
    }
}
