package org.jfrog.hudson;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This class represents specific configuration for a repository in {@link org.jfrog.hudson.ServerDetails}
 * @author Aviad Shikloshi
 */
public class RepositoryConf {

    private final String keyFromText;
    private final String keyFromSelect;
    private final boolean dynamicMode;

    /**
     * Data bound constructor to build RepositoryConf from Jenkins UI
     *
     * @param keyFromText   repository key retrieved from the text box of the repository
     * @param keyFromSelect repository key retrieved from the select box of the repository
     * @param dynamicMode   indicates where to take the repository key from - text or select box
     */
    @DataBoundConstructor
    public RepositoryConf(String keyFromText, String keyFromSelect, boolean dynamicMode) {
        this.keyFromText = keyFromText;
        this.keyFromSelect = keyFromSelect;
        this.dynamicMode = dynamicMode;
    }

    /**
     * Used to get the current repository key
     * @return keyFromText or keyFromSelect reflected by the dynamicMode flag
     */
    public String getRepoKey() {
        String repoKey;
        if (isDynamicMode()) {
            repoKey = keyFromText;
        } else {
            repoKey = keyFromSelect;
        }
        return repoKey;
    }

    public String getRepoName() {
        return getRepoKey();
    }

    public String getKeyFromText() {
        return keyFromText;
    }

    public String getKeyFromSelect() {
        return keyFromSelect;
    }

    /**
     * Check if the current mode for the Repository is dynamic (text box) or static (select box)
     * @return true if dynamic mode is used, false otherwise.
     */
    public boolean isDynamicMode() {
        // In Jenkins Job DSL we want to allow to not populate dynamicMode
        return dynamicMode || (StringUtils.isBlank(keyFromSelect) && StringUtils.isNotBlank(keyFromText));
    }

    // null object
    public static final RepositoryConf emptyRepositoryConfig = new RepositoryConf(StringUtils.EMPTY, StringUtils.EMPTY, false);
}
