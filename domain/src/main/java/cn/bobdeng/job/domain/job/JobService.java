package cn.bobdeng.job.domain.job;

import cn.bobdeng.job.domain.entity.*;

import java.util.List;

public interface JobService {
    void startJob(String jobId, String params);

    void onJobResult(JobResult jobResult);

    void retryJob(String jobId, String stepId);

    List<Job> getExecutingJob();

    List<JobStep> getExecutingJobSteps(String jobId);

    void executeStepByHttp(String jobId, String stepId, String host);

    void deleteJob(String jobId);

    List<JobConfig> getAllConfigJob();

    List<JobStepConfig> getJobConfigSteps(String jobId);

    void reloadJobConfig(String jobId);
}
