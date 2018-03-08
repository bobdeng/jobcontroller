package cn.bobdeng.job.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobStepConfig {
    private String jobId;
    private String stepId;
    private Integer delay;
    private String stepName;
    private int maxTry;
    private String methodName;
    private Set<String> preJobs;
    private int timeout;
}
