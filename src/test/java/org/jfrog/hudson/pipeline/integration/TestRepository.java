package org.jfrog.hudson.pipeline.integration;

import java.util.Arrays;

/**
 * @author yahavi
 */
enum TestRepository {
    LOCAL_REPO1("jenkins-tests-local-1"),
    LOCAL_REPO2("jenkins-tests-local-2"),
    JCENTER_REMOTE_REPO("jenkins-tests-jcenter"),
    NPM_LOCAL("jenkins-tests-npm-local"),
    NPM_REMOTE("jenkins-tests-npm-remote");

    private String repoName;

    TestRepository(String repoName) {
        this.repoName = repoName;
    }

    public String getRepoName() {
        return repoName;
    }

    public static String[] toArray() {
        return Arrays.stream(values()).map(TestRepository::getRepoName).toArray(String[]::new);
    }

    @Override
    public String toString() {
        return getRepoName();
    }
}
