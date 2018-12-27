/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;

/**
 * Used only as a placeholder for the release permissions.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryPlugin extends Plugin {

    private static final PermissionGroup GROUP =
            new PermissionGroup(ArtifactoryPlugin.class, Messages._permission_group());
    public static final Permission RELEASE = new Permission(GROUP, "Release",
            Messages._permission_release(), Hudson.ADMINISTER, PermissionScope.JENKINS);
    public static final Permission PROMOTE = new Permission(GROUP, "Promote",
            Messages._permission_promote(), Hudson.ADMINISTER, PermissionScope.JENKINS);
}
