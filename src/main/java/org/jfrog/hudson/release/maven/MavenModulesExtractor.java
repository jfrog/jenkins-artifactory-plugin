package org.jfrog.hudson.release.maven;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Alexei Vainshtein
 */
public class MavenModulesExtractor extends MasterToSlaveFileCallable<List<String>> {

    // This method is invoked by the FilePath object on the server that this job runs on.
    // This is needed when running on slaves.
    public List<String> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        MavenProject mavenProject = getMavenProject(f.getAbsolutePath());
        return mavenProject.getModel().getModules();
    }

    private MavenProject getMavenProject(String pomFile) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(pomFile), StandardCharsets.UTF_8.name())) {
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            Model model = mavenReader.read(reader);
            model.setPomFile(new File(pomFile));
            return new MavenProject(model);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }
}
