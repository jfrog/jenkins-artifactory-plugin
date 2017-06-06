package org.jfrog.hudson.release.maven;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * @author Alexei Vainshtein
 */
public class MavenModulesExtractor implements FilePath.FileCallable<List<String>> {

    // This been invoked by the FilePath object on the server that this job runs on.
    // This is needed if running on slaves.
    public List<String> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        MavenProject mavenProject = getMavenProject(f.getAbsolutePath());
        return mavenProject.getModel().getModules();
    }

    private MavenProject getMavenProject(String pomFile) throws IOException {
        try {
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            FileReader reader = new FileReader(pomFile);
            Model model = mavenReader.read(reader);
            model.setPomFile(new File(pomFile));
            return new MavenProject(model);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }
}
