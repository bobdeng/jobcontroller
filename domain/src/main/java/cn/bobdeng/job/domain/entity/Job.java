package cn.bobdeng.job.domain.entity;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Created by zhiguodeng on 2017/10/24.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Job {
    public static final int BASE_RETRY_DELAY_TIME_SECOND = 10;
    private String id;
    private String jobId;
    private String params;
    private String name;
    private long beginTime;
    private List<JobStep> steps;

    public static Job newJob(JobConfig jobConfig, List<JobStepConfig> jobStepConfigs, String params) {
        String jobSerial = UUID.randomUUID().toString();
        Job job = Job.builder()
                .jobId(jobConfig.getId())
                .id(jobSerial.toString())
                .name(jobConfig.getName())
                .beginTime(System.currentTimeMillis())
                .params(params)
                .build();
        List<JobStep> jobSteps = jobStepConfigs.stream()
                .map(jobStepConfig -> newJobStep(jobSerial, jobStepConfig))
                .collect(Collectors.toList());
        job.setSteps(jobSteps);
        return job;
    }

    private static JobStep newJobStep(String jobSerial, JobStepConfig jobStepConfig) {
        return JobStep.builder()
                .maxTry(jobStepConfig.getMaxTry())
                .jobId(jobStepConfig.getJobId())
                .executing(false)
                .delay(jobStepConfig.getDelay())
                .jobSerial(jobSerial)
                .methodName(jobStepConfig.getMethodName())
                .preJobs(jobStepConfig.getPreJobs())
                .stepId(jobStepConfig.getStepId())
                .stepName(jobStepConfig.getStepName())
                .tryTimes(0)
                .success(false)
                .build();
    }

    public boolean isOver() {
        return !steps.stream()
                .filter(jobStep -> !isJobStepOver(jobStep))
                .findFirst()
                .isPresent();
    }
    public boolean isJobSuccessOver() {
        return !steps.stream()
                .filter(jobStep -> !jobStep.isSuccess())
                .findAny()
                .isPresent();
    }

    /**
     * 获得失败重试的延迟重试时间
     *
     * @return 返回延迟重试时间（毫秒）
     */
    public long getDelayTime(JobStep step) {
        Preconditions.checkArgument(step.getTryTimes() < 6, "try times must < 6");
        if (step.getTryTimes() <= 1) {  // 第一次尝试延迟时间为0
            return step.getDelay();
        }
        return (long) Math.pow(BASE_RETRY_DELAY_TIME_SECOND, step.getTryTimes() - 1) * 1000;
    }

    public void runNextSteps(Consumer<JobStep> jobStepConsumer) {
        Set<String> overSteps = steps.stream()
                .filter(jobStep -> jobStep.isSuccess())
                .map(jobStep -> jobStep.getStepId())
                .collect(Collectors.toSet());
        steps.stream()
                .filter(jobStep -> isMyTurn(jobStep, overSteps))
                .forEach(jobStepConsumer);
    }
    public JobMethod getStepMethod(String stepId) {
        return Optional.ofNullable(getJobStep(stepId))
                .map(this::getJobMethod)
                .orElse(null);
    }

    private JobMethod getJobMethod(JobStep step) {
        return JobMethod.builder()
                .jobId(getJobId())
                .jobMethod(step.getMethodName())
                .jobSerialId(step.getJobSerial())
                .jobStepId(step.getStepId())
                .params(getParams())
                .build();
    }

    public void updateStepRuning(JobStep step) {
        step.setTryTimes(step.getTryTimes() + 1);
        step.setExecuting(true);
    }
    public void retryStep(String stepId) {
        steps.stream()
                .filter(jobExecuting -> jobExecuting.getStepId().equals(stepId))
                .findFirst()
                .ifPresent(this::resetTryTimes);
    }
    private void resetTryTimes(JobStep jobExecuting) {
        jobExecuting.setTryTimes(0);
        jobExecuting.setExecuting(false);
        jobExecuting.setSuccess(false);
    }
    public JobStep getJobStep(String stepId) {
        return steps.stream()
                .filter(jobExecuting -> jobExecuting.getStepId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("step not exit."));
    }
    private boolean isMyTurn(JobStep jobStep, Set<String> overSteps) {
        if (jobStep.isSuccess() || jobStep.isExecuting()) {
            return false;
        }
        if (jobStep.getTryTimes() >= jobStep.getMaxTry()) {
            return false;
        }
        if (jobStep.getPreJobs() == null) {
            return true;
        }
        if (jobStep.getPreJobs().stream()
                .filter(jobStepId -> !overSteps.contains(jobStepId))
                .findAny()
                .isPresent()) {
            return false;
        }
        return true;
    }

    private boolean isJobStepOver(JobStep jobStep) {
        return jobStep.isSuccess() || jobStep.getTryTimes() >= jobStep.getMaxTry();
    }

    public void updateStepResult(JobResult jobResult) {
        steps.stream()
                .filter(jobExecuting -> jobExecuting.getStepId().equals(jobResult.getJobStepId()))
                .findFirst()
                .ifPresent(jobExecuting -> updateJobResult(jobExecuting, jobResult));
    }
    private void updateJobResult(JobStep jobExecuting, JobResult jobResult) {
        jobExecuting.setSuccess(jobResult.isSuccess());
        jobExecuting.setExecuting(false);
    }

    public int getExecutingStepCount() {
        return (int)steps.stream()
                .filter(JobStep::isExecuting)
                .count();
    }

}
