package cn.bobdeng.job.domain.job;

import cn.bobdeng.job.domain.*;
import cn.bobdeng.job.domain.entity.*;
import cn.bobdeng.job.domain.interceptor.InterceptorDAO;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import lombok.Data;
import lombok.extern.java.Log;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;


@Log
@Data
public class JobServiceImpl implements JobService {
    public static final int JOB_HASH_EXPIRE_IN_MS = 60000;
    public static final String OK = "OK";
    private JobDAO jobDAO;
    private StepInvoker stepInvoker;
    private InterceptorDAO interceptorDAO;
    private DelayExecutor delayExecutor;

    public static final String HTTP_PREFIX = "http://";

    @Override
    public void startJob(String jobId, String params) {
        Job job = jobDAO.newJob(jobId, params);
        if (isRepeatJob(getJobRepeatHash(job))) {
            log.warning(() -> "repeat job:" + jobId + "," + params);
            return;
        }
        //加锁的原因是有结果太快回来导致问题
        lockJob(job.getId(), () -> {
            jobDAO.saveJob(job);
            jobDAO.putKey(getJobRepeatHash(job), OK, JOB_HASH_EXPIRE_IN_MS);
            try {
                doNext(job);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isRepeatJob(String hash) {
        return jobDAO.getKey(hash) != null;
    }

    private String getJobRepeatHash(Job job) {
        return Hashing.md5().hashString(job.getJobId() + job.getParams(), Charset.forName("utf-8")).toString();
    }


    private void lockJob(String jobId, Runnable runnable) {
        jobDAO.lock(jobId);
        try {
            runnable.run();
        } finally {
            jobDAO.releaseLock(jobId);
        }
    }

    private void doNext(Job job) {
        log.info(() -> "job doNext," + job);
        //得到已经完成的步骤
        if (job.isOver()) {
            onJobOver(job);
        } else {
            job.runNextSteps(jobStep -> {
                sendJob(jobStep, job);
            });
        }
    }

    /**
     * 发送执行命令出去
     *
     * @param step
     */
    private void sendJob(JobStep step, Job job) {
        Optional<StepInterceptor> interceptor = getInterceptor(job, step);
        if (interceptor.isPresent()) {
            if (!Strings.isNullOrEmpty(interceptor.get().getArbitraryServer())) {
                executeStepByHttp(job.getId(), step.getStepId(), interceptor.get().getArbitraryServer());
            }
        } else {
            if (step.isExecuting()) return;
            JobMethod stepMethod = job.getStepMethod(step.getStepId());
            job.updateStepRuning(step);
            jobDAO.saveSteps(job);
            long delayTime = job.getDelayTime(step);
            if (delayTime == 0) {
                log.info("sendJob with: step = [" + step.getJobSerial() + "], jobExecuting = [" + job.getName() + "]");
                stepInvoker.invoke(stepMethod);
            } else {
                log.info("delay sendJob with: step = [" + step.getJobSerial() + "], jobExecuting = [" + job.getName() + "], delay = [" + delayTime + "]");
                delayExecutor.execute(() -> stepInvoker.invoke(stepMethod), delayTime);
            }
        }
    }

    private Optional<StepInterceptor> getInterceptor(Job job, JobStep step) {
        return interceptorDAO.getStepInterceptors()
                .stream()
                .filter(stepInterceptor -> stepInterceptor.getJobId().equals(job.getJobId()))
                .filter(stepInterceptor -> stepInterceptor.getJobStepId().equals(step.getStepId()))
                .findAny();
    }

    @Override
    public void onJobResult(JobResult jobResult) {
        lockJob(jobResult.getJobSerialId(), () -> {
            Job job = jobDAO.getJob(jobResult.getJobSerialId());
            job.updateStepResult(jobResult);
            jobDAO.saveSteps(job);
            if (!Strings.isNullOrEmpty(jobResult.getOutput())) {
                job.setParams(jobResult.getOutput());
                jobDAO.saveJob(job);
            }
            if (job.isOver()) {
                log.info("job over id : " + job.getId() + ", job steps: " + job);
                onJobOver(job);
            } else {
                if (job.getExecutingStepCount() == 0) {
                    doNext(job);
                } else {
                    log.info("job executing:" + job.getExecutingStepCount() + ", job id: " + job.getJobId());
                }
            }
        });
    }

    @Override
    public void retryJob(String jobId, String stepId) {
        lockJob(jobId, () -> {
            Job job = jobDAO.getJob(jobId);
            job.retryStep(stepId);
            this.doNext(job);
        });
    }

    @Override
    public List<Job> getExecutingJob() {
        return jobDAO.findAll();
    }

    @Override
    public List<JobStep> getExecutingJobSteps(String jobId) {
        return jobDAO.getJob(jobId).getSteps();
    }

    private void onJobOver(Job job) {
        log.info(() -> "job over: " + job.getId());
        if (job.isJobSuccessOver()) {
            jobDAO.deleteJobById(job.getJobId());
        }
    }

    @Override
    public void executeStepByHttp(String jobId, String stepId, String host) {
        host = host.startsWith(HTTP_PREFIX) ? host : HTTP_PREFIX + host;
        Job job = jobDAO.getJob(jobId);
        JobMethod jobMethod = job.getStepMethod(stepId);
        stepInvoker.invokeJobMethodViaHttp(host, jobMethod);
    }

    @Override
    public void deleteJob(String jobId) {
        jobDAO.deleteJobById(jobId);
    }

    @Override
    public List<JobConfig> getAllConfigJob() {
        return jobDAO.getAllJobConfig();
    }

    @Override
    public List<JobStepConfig> getJobConfigSteps(String jobId) {
        return jobDAO.getJobConfigSteps(jobId);
    }

    @Override
    public void reloadJobConfig(String jobId) {
        jobDAO.reloadJobConfig(jobId);
    }
}
