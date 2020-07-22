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

package org.jfrog.hudson.ivy;

import hudson.Extension;
import hudson.ivy.IvyModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.jfrog.hudson.BuildInfoResultAction;
import org.jfrog.hudson.release.promotion.UnifiedPromoteBuildAction;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;


/**
 * A build listener which takes care of Ivy builds. This class extends a {@link hudson.model.listeners.RunListener} with
 * a generic type of {@link hudson.model.AbstractBuild} and not {@link hudson.ivy.IvyModuleSetBuild} since Hudson's
 * classloader tries to load initialize the class by reflection and is failing on {@link LinkageError} which is handled
 * by Hudson by printing out the stacktrace to the log. However, if not using it during construction time and checking
 * at runtime, the exception seems to disappear.
 *
 * @author Tomer Cohen
 */
@Extension(optional = true)
public class ArtifactoryIvyRunListener extends RunListener<AbstractBuild> {
    public ArtifactoryIvyRunListener() {
        super(AbstractBuild.class);
    }

    @Override
    public void onCompleted(AbstractBuild run, TaskListener listener) {
        if ("hudson.ivy.IvyModuleSetBuild".equals(run.getClass().getName())) {
            IvyModuleSetBuild ivyRun = (IvyModuleSetBuild) run;
            Result result = ivyRun.getResult();
            if (result == null || result.isWorseThan(Result.SUCCESS)) {
                return;
            }
            ArtifactoryIvyConfigurator artifactoryIvyConfigurator =
                    ivyRun.getProject().getBuildWrappersList().get(ArtifactoryIvyConfigurator.class);
            if (artifactoryIvyConfigurator == null) {
                return;
            }
            if (artifactoryIvyConfigurator.isDeployBuildInfo()) {
                String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(artifactoryIvyConfigurator, run, null);
                run.getActions().add(new BuildInfoResultAction(artifactoryIvyConfigurator.getArtifactoryUrl(), run, buildName));
                run.getActions().add(new UnifiedPromoteBuildAction(run, artifactoryIvyConfigurator));
            }
        }
    }
}
