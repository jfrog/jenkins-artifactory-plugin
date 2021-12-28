package org.jfrog.hudson.release;

import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
public class PromotionConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String targetRepository;
    private String comment;

    public PromotionConfig(String targetRepository, String comment) {
        this.targetRepository = targetRepository;
        this.comment = comment;
    }

    public String getTargetRepository() {
        return targetRepository;
    }

    public String getComment() {
        return comment;
    }
}
