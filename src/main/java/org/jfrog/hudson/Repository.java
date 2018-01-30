package org.jfrog.hudson;

/**
 * @author Aviad Shikloshi
 */
public class Repository implements Comparable<Repository> {

    private String value;

    public Repository(String repositoryKey) {
        this.value = repositoryKey;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(Repository r) {
        return this.value.compareTo(r.getValue());
    }
}
