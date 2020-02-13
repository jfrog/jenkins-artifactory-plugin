package org.jfrog.hudson.pipeline.integration;

/**
 * @author yahavi
 */
enum TestRepository {
    LOCAL_REPO1("jenkins-artifactory-tests-local-1", RepoType.LOCAL),
    LOCAL_REPO2("jenkins-artifactory-tests-local-2", RepoType.LOCAL),
    JCENTER_REMOTE_REPO("jenkins-artifactory-tests-jcenter", RepoType.REMOTE),
    NPM_LOCAL("jenkins-artifactory-tests-npm-local", RepoType.LOCAL),
    NPM_REMOTE("jenkins-artifactory-tests-npm-remote", RepoType.REMOTE),
    GO_LOCAL("jenkins-artifactory-tests-go-local", RepoType.LOCAL),
    GO_REMOTE("jenkins-artifactory-tests-go-remote", RepoType.REMOTE),
    GO_VIRTUAL("jenkins-artifactory-tests-go-virtual", RepoType.VIRTUAL);

    enum RepoType {
        LOCAL,
        REMOTE,
        VIRTUAL
    }

    private String repoName;
    private RepoType repoType;

    TestRepository(String repoName, RepoType repoType) {
        this.repoName = repoName;
        this.repoType = repoType;
    }

    public RepoType getRepoType() {
        return repoType;
    }

    public String getRepoName() {
        return repoName;
    }

    @Override
    public String toString() {
        return getRepoName();
    }
}
