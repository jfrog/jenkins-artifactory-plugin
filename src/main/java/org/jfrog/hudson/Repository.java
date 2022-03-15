package org.jfrog.hudson;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

    @SuppressFBWarnings(value = "EQ_COMPARETO_USE_OBJECT_EQUALS")
    public int compareTo(Repository r) {
        return this.value.compareTo(r.getValue());
    }
}
