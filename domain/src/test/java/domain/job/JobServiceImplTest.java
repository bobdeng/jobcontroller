package domain.job;

import cn.bobdeng.job.domain.entity.Job;
import cn.bobdeng.job.domain.entity.JobConfig;
import cn.bobdeng.job.domain.entity.JobResult;
import cn.bobdeng.job.domain.entity.JobStepConfig;
import cn.bobdeng.job.domain.interceptor.InterceptorDAO;
import cn.bobdeng.job.domain.job.DelayExecutor;
import cn.bobdeng.job.domain.job.JobDAO;
import cn.bobdeng.job.domain.job.JobServiceImpl;
import cn.bobdeng.job.domain.job.StepInvoker;
import com.google.common.hash.Hashing;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JobServiceImplTest {
    private static final Integer DELAY_3 = 0;
    private static final int MAX_TRY_3 = 3;
    private static final String METHOD_3 = "method_3";
    private static final String STEP_3 = "step_3";
    private static final String STEP_NAME_3 = "step_name_3";
    @InjectMocks
    JobServiceImpl jobService;
    @Mock
    private JobDAO jobDAO;
    @Mock
    private StepInvoker stepInvoker;
    @Mock
    private InterceptorDAO interceptorDAO;
    @Mock
    private DelayExecutor delayExecutor;
    public static final String PARAMS = "{'params':1}";
    public static final int DELAY_1 = 1000;
    public static final String JOB_ID = "job_id";
    public static final String JOB_NAME = "job_name";
    public static final String METHOD_1 = "method_1";
    public static final String STEP_1 = "step_1";
    public static final String STEP_NAME_1 = "step_name_1";
    private static final Integer DELAY_2 = 0;
    public static final int MAX_TRY_1 = 3;
    public static final int MAX_TRY_2 = 2;
    private static final String METHOD_2 = "method_2";
    private static final String STEP_2 = "step_2";
    private static final String STEP_NAME_2 = "step_name_2";
    public static final String NEW_PARAMS = "{new_params}";
    @Captor
    ArgumentCaptor<Runnable> runnableArgumentCaptor;
    @Captor
    ArgumentCaptor<Long> delayCaptor;
    @Test
    public void testConstants(){

    }
    private Job createNewJob() {
        return Job.newJob(JobConfig.builder()
                .id(JOB_ID)
                .name(JOB_NAME)
                .build(), createJobSteps(), PARAMS);
    }

    private List<JobStepConfig> createJobSteps() {
        return Arrays.asList(
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
                        .preJobs(null)
                        .stepId(STEP_2)
                        .stepName(STEP_NAME_2)
                        .build(),
                JobStepConfig.builder()
                        .delay(DELAY_3)
                        .maxTry(MAX_TRY_3)
                        .methodName(METHOD_3)
                        .preJobs(Stream.of(STEP_1,STEP_2).collect(Collectors.toSet()))
                        .stepId(STEP_3)
                        .stepName(STEP_NAME_3)
                        .build()
        );
    }

    @Test
    public void startJob() {
        Job newJob = createNewJob();
        when(jobDAO.newJob(JOB_ID,PARAMS)).thenReturn(newJob);
        when(jobDAO.getKey(getJobRepeatHash(JOB_ID,PARAMS))).thenReturn(null);

        jobService.startJob(JOB_ID,PARAMS);

        verify(jobDAO).saveJob(newJob);
        verify(delayExecutor).execute(runnableArgumentCaptor.capture(),delayCaptor.capture());
        verify(stepInvoker).invoke(newJob.getStepMethod(STEP_2));
        verify(stepInvoker,times(0)).invoke(newJob.getStepMethod(STEP_1));
        runnableArgumentCaptor.getValue().run();
        verify(stepInvoker).invoke(newJob.getStepMethod(STEP_1));
        assertEquals(delayCaptor.getValue().longValue(),DELAY_1);
    }
    @Test
    public void startJob_repeat() {
        Job newJob = createNewJob();
        when(jobDAO.newJob(JOB_ID,PARAMS)).thenReturn(newJob);
        when(jobDAO.getKey(getJobRepeatHash(JOB_ID,PARAMS))).thenReturn("OK");

        jobService.startJob(JOB_ID,PARAMS);

        verify(jobDAO,times(0)).saveJob(any());
        verifyZeroInteractions(delayExecutor);
        verifyZeroInteractions(stepInvoker);
    }
    private String getJobRepeatHash(String jobId,String params) {
        return Hashing.md5().hashString(jobId +params, Charset.forName("utf-8")).toString();
    }

    @Test
    public void onJobResult() {
        Job newJob = createNewJob();
        newJob.getJobStep(STEP_1).setExecuting(true);
        newJob.getJobStep(STEP_2).setExecuting(true);
        when(jobDAO.getJob(newJob.getId())).thenReturn(newJob);
        jobService.onJobResult(JobResult.builder()
                .jobStepId(STEP_1)
                .output(NEW_PARAMS)
                .success(true)
                .jobSerialId(newJob.getId())
                .build());
        jobService.onJobResult(JobResult.builder()
                .jobStepId(STEP_2)
                .output(NEW_PARAMS)
                .success(true)
                .jobSerialId(newJob.getId())
                .build());

        assertEquals(newJob.getParams(),NEW_PARAMS);
        verify(stepInvoker).invoke(newJob.getStepMethod(STEP_3));

    }
    @Test
    public void onJobResult_fail() {
        Job newJob = createNewJob();
        newJob.getJobStep(STEP_1).setExecuting(true);
        newJob.getJobStep(STEP_2).setExecuting(true);
        when(jobDAO.getJob(newJob.getId())).thenReturn(newJob);
        jobService.onJobResult(JobResult.builder()
                .jobStepId(STEP_1)
                .output(NEW_PARAMS)
                .success(true)
                .jobSerialId(newJob.getId())
                .build());
        jobService.onJobResult(JobResult.builder()
                .jobStepId(STEP_2)
                .output(NEW_PARAMS)
                .success(false)
                .jobSerialId(newJob.getId())
                .build());

        assertEquals(newJob.getParams(),NEW_PARAMS);
        verify(stepInvoker).invoke(newJob.getStepMethod(STEP_2));

    }
    @Test
    public void onJobResult_over() {
        Job newJob = createNewJob();
        newJob.getJobStep(STEP_1).setExecuting(false).setSuccess(true);
        newJob.getJobStep(STEP_2).setExecuting(false).setSuccess(true);
        when(jobDAO.getJob(newJob.getId())).thenReturn(newJob);
        jobService.onJobResult(JobResult.builder()
                .jobStepId(STEP_3)
                .output(NEW_PARAMS)
                .success(true)
                .jobSerialId(newJob.getId())
                .build());

        verifyZeroInteractions(stepInvoker);

    }

    @Test
    public void retryJob() {

    }

    @Test
    public void getExecutingJob() {

    }

    @Test
    public void getExecutingJobSteps() {

    }

    @Test
    public void executeStepByHttp() {

    }

    @Test
    public void deleteJob() {
    }

    @Test
    public void getAllConfigJob() {
    }

    @Test
    public void getJobConfigSteps() {
    }

    @Test
    public void reloadJobConfig() {
    }
}