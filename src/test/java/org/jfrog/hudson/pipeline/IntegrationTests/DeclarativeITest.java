package org.jfrog.hudson.pipeline.IntegrationTests;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;

import java.nio.file.Files;
import java.util.Arrays;

import static org.jfrog.hudson.pipeline.IntegrationTests.ITestUtils.*;
import static org.junit.Assert.*;

public class DeclarativeITest extends PipelineITestBase {

    public DeclarativeITest() {
        super(PipelineType.DECLARATIVE);
    }

    @Test
    public void uploadTest() throws Exception {
        buildWorkflowProject("upload");
        for (String fileName : Arrays.asList("a.in", "b.in", "c.in")) {
            assertTrue(isExistInArtifactory(artifactoryClient, localRepo, fileName));
        }
    }

    @Test
    public void uploadFailNoOpTest() throws Exception {
        try {
            buildWorkflowProject("uploadFailNoOp");
            fail("Job expected to fail");
        } catch (AssertionError t) {
            assertTrue(t.getMessage().contains("Fail-no-op: No files were affected in the upload process."));
        }
        for (String fileName : Arrays.asList("a.in", "b.in", "c.in")) {
            assertFalse(isExistInArtifactory(artifactoryClient, localRepo, fileName));
        }
    }

    @Test
    public void downloadTest() throws Exception {
        Files.list(FILES_PATH).forEach(file -> uploadFile(artifactoryClient, file, localRepo));
        WorkflowRun build = buildWorkflowProject("download");
        for (String fileName : Arrays.asList("a.in", "b.in", "c.in")) {
            assertTrue(isExistInWorkspace(jenkins, build, "download-test", fileName));
        }
    }

    @Test
    public void downloadFailNoOpTest() throws Exception {
        try {
            buildWorkflowProject("downloadFailNoOp");
            fail("Job expected to fail");
        } catch (AssertionError t) {
            assertTrue(t.getMessage().contains("Fail-no-op: No files were affected in the download process."));
        }
    }
}