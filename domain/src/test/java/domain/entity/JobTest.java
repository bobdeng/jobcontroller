package domain.entity;

import cn.bobdeng.job.domain.entity.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;


@RunWith(JUnit4.class)
public class JobTest {

    public static final String PARAMS = "{'params':1}";
    public static final int DELAY_1 = 1000;
    public static final String JOB_ID = "job_id";
    public static final String JOB_NAME = "job_name";
    public static final String METHOD_1 = "method_1";
    public static final String STEP_1 = "step_1";
    public static final String STEP_NAME_1 = "step_name_1";
    private static final Integer DELAY_2 = 1001;
    public static final int MAX_TRY_1 = 3;
    public static final int MAX_TRY_2 = 2;
    private static final String METHOD_2 = "method_2";
    private static final String STEP_2 = "step_2";
    private static final String STEP_NAME_2 = "step_name_2";
    public static final String NEW_PARAMS = "{new_params}";

    @Test
    public void newJob() {
        Job job = createNewJob();
        assertEquals(job.getJobId(),JOB_ID);
        assertNotNull(job.getJobId());
        assertEquals(job.getName(),JOB_NAME);
        assertEquals(job.getSteps().size(),2);
        assertFalse(job.isOver());
        assertFalse(job.isJobSuccessOver());
        JobStep jobStep1=job.getJobStep(STEP_1);
        assertEquals(jobStep1.getTryTimes(),0);
        assertEquals(jobStep1.getDelay(),DELAY_1);
        assertFalse(jobStep1.isSuccess());
        assertEquals(jobStep1.getMaxTry(),MAX_TRY_1);
        assertNull(jobStep1.getPreJobs());
        assertEquals(jobStep1.getMethodName(),METHOD_1);
        assertEquals(jobStep1.getStepName(),STEP_NAME_1);
        JobStep jobStep2=job.getJobStep(STEP_2);
        assertTrue(jobStep2.getPreJobs().contains(STEP_1));
    }

    private Job createNewJob() {
        return Job.newJob(JobConfig.builder()
                .id(JOB_ID)
                .name(JOB_NAME)
                .build(), Arrays.asList(
                JobStepConfig.builder()
                        .delay(DELAY_1)
                        .maxTry(MAX_TRY_1)
                        .methodName(METHOD_1)
                        .preJobs(null)
                        .stepId(STEP_1)
                        .stepName(STEP_NAME_1)
                        .build(),
                JobStepConfig.builder()
                        .delay(DELAY_2)
                        .maxTry(MAX_TRY_2)
                        .methodName(METHOD_2)
                        .preJobs(Stream.of(STEP_1).collect(Collectors.toSet()))
                        .stepId(STEP_2)
                        .stepName(STEP_NAME_2)
                        .build()
        ), PARAMS);
    }

    @Test
    public void isOver_SuccessOver() {
        Job job=createNewJob();
        job.getJobStep(STEP_1).setSuccess(true);
        job.getJobStep(STEP_2).setSuccess(true);
        assertTrue(job.isOver());
        assertTrue(job.isJobSuccessOver());
    }
    @Test
    public void isOver_OverWithError() {
        Job job=createNewJob();
        job.getJobStep(STEP_1).setSuccess(true);
        job.getJobStep(STEP_2).setSuccess(false);
        job.getJobStep(STEP_2).setTryTimes(MAX_TRY_2);
        assertTrue(job.isOver());
        assertFalse(job.isJobSuccessOver());
    }
    @Test
    public void isOver_NotOver() {
        Job job=createNewJob();
        job.getJobStep(STEP_1).setSuccess(true);
        job.getJobStep(STEP_2).setSuccess(false);
        job.getJobStep(STEP_2).setTryTimes(0);
        assertFalse(job.isOver());
        assertFalse(job.isJobSuccessOver());
    }

    @Test
    public void getExecutingStepCount() {
        Job job=createNewJob();
        JobStep jobStep = job.getJobStep(STEP_1);
        jobStep.setExecuting(true);
        assertEquals(job.getExecutingStepCount(),1);
    }

    @Test
    public void getRetryDelayTime() {
        Job job=createNewJob();
        JobStep jobStep = job.getJobStep(STEP_1);
        jobStep.setTryTimes(1);
        assertEquals(job.getDelayTime(jobStep),DELAY_1);
        jobStep.setTryTimes(2);
        assertTrue(job.getDelayTime(jobStep)>DELAY_1);
    }

    @Test
    public void runNextSteps_first() {
        Job job=createNewJob();
        List<JobStep> running=new ArrayList();
        job.runNextSteps(running::add);
        assertEquals(running.get(0),job.getJobStep(STEP_1));
        assertEquals(running.size(),1);
    }
    @Test
    public void runNextSteps_next() {
        Job job=createNewJob();
        job.getJobStep(STEP_1).setSuccess(true);
        List<JobStep> running=new ArrayList();
        job.runNextSteps(running::add);
        assertEquals(running.get(0),job.getJobStep(STEP_2));
        assertEquals(running.size(),1);
    }

    @Test
    public void getStepMethod() {
        Job job=createNewJob();
        JobMethod stepMethod = job.getStepMethod(STEP_1);
        JobStep jobStep = job.getJobStep(STEP_1);
        assertEquals(stepMethod.getJobId(),job.getJobId());
        assertEquals(stepMethod.getJobMethod(),jobStep.getMethodName());
        assertEquals(stepMethod.getJobSerialId(),job.getId());
        assertEquals(stepMethod.getJobStepId(),STEP_1);
        assertEquals(stepMethod.getParams(),job.getParams());

    }

    @Test
    public void updateStepRunning() {
        Job job=createNewJob();
        job.updateStepRuning(job.getJobStep(STEP_1));
        JobStep jobStep = job.getJobStep(STEP_1);
        assertTrue(jobStep.isExecuting());
    }

    @Test
    public void retryStep() {
        Job job=createNewJob();
        JobStep jobStep = job.getJobStep(STEP_1);
        jobStep.setTryTimes(3);
        jobStep.setExecuting(true);
        job.retryStep(STEP_1);
        assertFalse(jobStep.isExecuting());
        assertFalse(jobStep.isSuccess());
        assertEquals(jobStep.getTryTimes(),0);
    }

    @Test
    public void updateStepResult_success() {
        Job job=createNewJob();
        job.updateStepResult(JobResult.builder()
                .success(true)
                .jobStepId(STEP_1)
                .output(NEW_PARAMS)
                .build());
        JobStep jobStep = job.getJobStep(STEP_1);
        assertFalse(jobStep.isExecuting());
        assertTrue(jobStep.isSuccess());
    }
    @Test
    public void updateStepResult_fail() {
        Job job=createNewJob();
        job.updateStepResult(JobResult.builder()
                .success(false)
                .jobStepId(STEP_1)
                .output(NEW_PARAMS)
                .build());
        JobStep jobStep = job.getJobStep(STEP_1);
        assertFalse(jobStep.isExecuting());
        assertFalse(jobStep.isSuccess());
    }

}