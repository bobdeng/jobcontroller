package cn.bobdeng.job.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class JobStep {
    private String jobSerial;
    private String jobId;
    private String stepId;
    private boolean success;
    private Set<String> preJobs;
    private String stepName;
    private int tryTimes;
    private int delay;//延迟
    private int maxTry;
    private String methodName;
    private boolean executing;
}
