package org.jfrog.hudson.pipeline.integrationtests;

import org.junit.Test;

public class DeclarativeITest extends PipelineITestBase {

    public DeclarativeITest() {
        super(PipelineType.DECLARATIVE);
    }

    @Test
    public void downloadTest() throws Exception {
        super.downloadTest("declarative-download-test");
    }

    @Test
    public void uploadTest() throws Exception {
        super.uploadTest("declarative-upload-test");
    }

    @Test
    public void promotionTest() throws Exception {
        super.promotionTest("declarative-promotion-test");
    }
}
