package org.jfrog.hudson.trigger;

import antlr.ANTLRException;
import hudson.model.BuildableItem;
import hudson.model.Job;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jfrog.hudson.ServerDetails;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author yahavi
 **/
public class ArtifactoryMultibranchTriggerTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testGetJobsToTriggerNoBranches() throws ANTLRException {
        ArtifactoryMultibranchTrigger multibranchTrigger = createArtifactoryMultibranchTriggerMock("");

        // Make sure no errors occur when there are no jobs in multibranch pipeline
        List<WorkflowJob> jobs = new ArrayList<>();
        List<BuildableItem> jobsToTrigger = multibranchTrigger.getJobsToTrigger(jobs);
        assertTrue(jobsToTrigger.isEmpty());

        // Make sure all jobs are returned when an empty branch list is used
        jobs.add(new WorkflowJob(null, "branch1"));
        jobs.add(new WorkflowJob(null, "branch2"));
        jobsToTrigger = multibranchTrigger.getJobsToTrigger(jobs);
        assertEquals(1, jobsToTrigger.size());
    }

    @Test
    public void testGetJobsToTriggerOneBranch() throws ANTLRException {
        ArtifactoryMultibranchTrigger multibranchTrigger = createArtifactoryMultibranchTriggerMock("master");

        // Make sure no errors occur when there are no jobs in multibranch pipeline
        List<WorkflowJob> jobs = new ArrayList<>();
        List<BuildableItem> jobsToTrigger = multibranchTrigger.getJobsToTrigger(jobs);
        assertTrue(jobsToTrigger.isEmpty());

        // Make sure the "master" branch is returned
        jobs.add(new WorkflowJob(null, "master"));
        jobsToTrigger = multibranchTrigger.getJobsToTrigger(jobs);
        assertEquals(1, jobsToTrigger.size());

        // Make sure that only the "master" branch is returned
        jobs.add(new WorkflowJob(null, "dev"));
        jobsToTrigger = multibranchTrigger.getJobsToTrigger(jobs);
        assertEquals(1, jobsToTrigger.size());
        assertEquals("master", jobsToTrigger.get(0).getName());
    }

    @Test
    public void testGetJobsToTriggerTwoBranches() throws ANTLRException {
        ArtifactoryMultibranchTrigger multibranchTrigger = createArtifactoryMultibranchTriggerMock("master;dev");

        // Make sure no errors occur when there are no jobs in multibranch pipeline
        List<WorkflowJob> jobs = new ArrayList<>();
        List<BuildableItem> jobsToTrigger = multibranchTrigger.getJobsToTrigger(jobs);
        assertTrue(jobsToTrigger.isEmpty());

        // Make sure the "master" branch is returned
        jobs.add(new WorkflowJob(null, "master"));
        jobsToTrigger = multibranchTrigger.getJobsToTrigger(jobs);
        assertEquals(1, jobsToTrigger.size());

        // Make sure the "master" and "dev" branches are returned
        jobs.add(new WorkflowJob(null, "dev"));
        jobsToTrigger = multibranchTrigger.getJobsToTrigger(jobs);
        assertEquals(2, jobsToTrigger.size());

        // Make sure only the "master" and "dev" branches are returned
        jobs.add(new WorkflowJob(null, "dev2"));
        jobsToTrigger = multibranchTrigger.getJobsToTrigger(jobs);
        assertEquals(2, jobsToTrigger.size());
        assertTrue("master".equals(jobsToTrigger.get(0).getName()) || "dev".equals(jobsToTrigger.get(0).getName()));
        assertTrue("master".equals(jobsToTrigger.get(1).getName()) || "dev".equals(jobsToTrigger.get(1).getName()));
    }

    private ArtifactoryMultibranchTrigger createArtifactoryMultibranchTriggerMock(String branches) throws ANTLRException {
        ArtifactoryMultibranchTrigger multibranchTrigger = new ArtifactoryMultibranchTriggerMock(null, "", "", branches);
        multibranchTrigger.start(new WorkflowMultiBranchProject(jenkins.jenkins, ""), true);
        return multibranchTrigger;
    }

    /**
     * We extends the test class only to override the getBranchName method.
     */
    private static class ArtifactoryMultibranchTriggerMock extends ArtifactoryMultibranchTrigger {

        public ArtifactoryMultibranchTriggerMock(ServerDetails details, String paths, String spec, String branches) throws ANTLRException {
            super(details, paths, spec, branches);
        }

        @Override
        String getBranchName(Job<?, ?> branchJob) {
            return branchJob.getName();
        }
    }
}
