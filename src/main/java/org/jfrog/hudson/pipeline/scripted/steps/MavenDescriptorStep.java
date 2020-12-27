package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.build.extractor.maven.reader.ModuleName;
import org.jfrog.build.extractor.maven.transformer.PomTransformer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by tamirh on 20/11/2016.
 */
public class MavenDescriptorStep extends AbstractStepImpl {
    private String pomFile = "pom.xml";
    private String version = "";
    private Map<String, String> versionPerModule = new HashedMap();
    private boolean failOnSnapshot;
    private boolean dryRun;


    @DataBoundConstructor
    public MavenDescriptorStep(String pomFile, String version, Map<String, String> versionPerModule, boolean failOnSnapshot, boolean dryRun) {
        this.pomFile = pomFile;
        this.version = version;
        this.versionPerModule = versionPerModule;
        this.failOnSnapshot = failOnSnapshot;
        this.dryRun = dryRun;
    }

    public String getPomFile() {
        return pomFile;
    }

    public String getVersion() {
        return version;
    }

    public boolean isFailOnSnapshot() {
        return failOnSnapshot;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public Map<String, String> getVersionPerModule() {
        return versionPerModule;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Boolean> {

        private transient MavenDescriptorStep step;
        private String pomFile;
        private boolean failOnSnapshot;
        private boolean dryRun;
        private String version = "";
        private Map<String, String> versionPerModule = new HashedMap();

        @Inject
        public Execution(MavenDescriptorStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }


        @Override
        protected Boolean runStep() throws Exception {
            pomFile = new FilePath(ws, step.getPomFile()).getRemote();
            failOnSnapshot = step.isFailOnSnapshot();
            dryRun = step.isDryRun();
            this.version = step.getVersion();
            this.versionPerModule = step.getVersionPerModule();
            Boolean call = launcher.getChannel().call(new MasterToSlaveCallable<Boolean, IOException>() {
                public Boolean call() throws IOException {
                    return transformPoms();
                }
            });
            return call;
        }

        public boolean transformPoms() {
            String filePath = "";
            String fileName = pomFile;
            int index = StringUtils.lastIndexOf(pomFile, File.separator);
            if (index > 0) {
                filePath = StringUtils.substring(pomFile, 0, index + 1);
                fileName = StringUtils.substring(pomFile, index + 1);
            }

            final Map<ModuleName, String> modules = new HashedMap();
            Map<ModuleName, String> modulesVersion = new HashedMap();

            findPomModules(filePath, fileName, modules);
            return execTransformtion(modules, modulesVersion);
        }

        private boolean execTransformtion(Map<ModuleName, String> modules, Map<ModuleName, String> modulesVersion) {
            boolean isTransformed = false;
            for (Map.Entry<ModuleName, String> module : modules.entrySet()) {
                modulesVersion.put(module.getKey(), getModuleVersion(module.getKey()));
            }
            for (Map.Entry<ModuleName, String> module : modules.entrySet()) {
                PomTransformer pomTransformer = new PomTransformer(module.getKey(), modulesVersion, null, failOnSnapshot, dryRun);
                try {
                    isTransformed |= pomTransformer.transform(new File(module.getValue()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return isTransformed;
        }

        private void findPomModules(String filePath, String fileName, Map<ModuleName, String> result) {
            Model model;
            String pomPath = filePath + fileName;
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(pomPath));
                MavenXpp3Reader reader = new MavenXpp3Reader();
                model = reader.read(in);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                        // Ignored
                    }
                }
            }
            String parentGroupId = null;
            String parentArtifactId = null;
            if (model.getParent() != null) {
                parentArtifactId = model.getParent().getArtifactId();
                parentGroupId = model.getParent().getGroupId();
            }

            String groupId = model.getGroupId() != null ? model.getGroupId() : parentGroupId;
            String artifactId = model.getArtifactId() != null ? model.getArtifactId() : parentArtifactId;
            if (groupId == null || artifactId == null) {
                throw new IllegalStateException("artifactId and/or groupId could not be found in POM file: " + pomPath);
            }

            result.put(new ModuleName(groupId, artifactId), pomPath);
            List<String> modules = model.getModules();
            for (String module : modules) {
                String tempFilePath = StringUtils.endsWith(filePath, File.separator) ? filePath + module + File.separator : filePath + File.separator + module + File.separator;
                findPomModules(tempFilePath, "pom.xml", result);
            }
        }

        private String getModuleVersion(ModuleName module) {
            String moduleIdentifier = module.getGroupId() + ":" + module.getArtifactId();
            String version = versionPerModule.get(moduleIdentifier);
            if (StringUtils.isNotEmpty(version)) {
                return version;
            }
            if (StringUtils.isEmpty(this.version)) {
                throw new RuntimeException("Can't find version for module" + moduleIdentifier + ".");
            }
            return this.version;
        }

    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(MavenDescriptorStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "MavenDescriptorStep";
        }

        @Override
        public String getDisplayName() {
            return "Get Artifactory Maven descriptor";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
