package org.jfrog.hudson.util.plugins;

import hudson.matrix.Combination;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.Map;

/**
 * @author Lior Hasson
 */
public class MultiConfigurationUtils {
    public static void validateCombinationFilter(AbstractBuild build, BuildListener listener, String combFilter) {
        if (StringUtils.isBlank(combFilter)) {
            String error = "The \"Combination Matches\" field is mandatory. It cannot be empty.";
            listener.getLogger().println(error);
            throw new IllegalArgumentException(error);
        }
    }

    public static boolean isfiltrated(final AbstractBuild build, String combinationFilter) {
        //Empty combination consider as filter all
        if (StringUtils.isEmpty(combinationFilter))
            return true;

        if (build.getProject() instanceof MatrixConfiguration) {
            MatrixConfiguration matrixConf = ((MatrixConfiguration) build.getProject());
            return !matrixConf.getCombination().
                    evalGroovyExpression(matrixConf.getParent().getAxes(), combinationFilter);
        }

        return false;
    }

    public static void addMatrixCombination(Run<?, ?> build, ArtifactoryClientConfiguration configuration) {
        if (build.getParent() instanceof MatrixConfiguration) {
            Combination combination = ((MatrixConfiguration) build.getParent()).getCombination();
            for (Map.Entry<String, String> entries : combination.entrySet()) {
                configuration.info.addRunParameters(entries.getKey(), entries.getValue());
            }
        }
    }
}
