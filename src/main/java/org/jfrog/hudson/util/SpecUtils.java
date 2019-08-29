package org.jfrog.hudson.util;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.SpecConfiguration;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by diman on 16/10/2016.
 */
public class SpecUtils {

    public static String getSpecStringFromSpecConf(SpecConfiguration specConfiguration, EnvVars env, FilePath workspace, PrintStream logger)
            throws IOException, InterruptedException {

        if (StringUtils.isNotBlank(specConfiguration.getFilePath())) {
            String filePath = specConfiguration.getFilePath().trim();
            filePath = Util.replaceMacro(filePath, env);
            String spec = buildSpecPath(filePath, workspace, logger).readToString();
            return Util.replaceMacro(spec.trim(), env);
        }
        if (StringUtils.isNotBlank(specConfiguration.getSpec())) {
            return Util.replaceMacro(specConfiguration.getSpec().trim(), env);
        }
        return "";
    }

    private static FilePath buildSpecPath(String providedPath, FilePath workingDir, PrintStream logger)
            throws IOException, InterruptedException {

        FilePath relativeFile = new FilePath(workingDir, providedPath);
        if (relativeFile.exists() && !relativeFile.isDirectory()) {
            logger.println(String.format("Using spec file: %s", relativeFile.getRemote()));
            return relativeFile;
        }

        throw new IOException(String.format("Could not find spec file in the provided path: %s", relativeFile.getRemote()));
    }
}