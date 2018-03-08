package cn.bobdeng.job.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by zhiguodeng on 2017/10/24.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobMethod {
    private String jobSerialId;//作业唯一的ID
    private String jobId;//作业ID
    private String jobStepId;//作业步骤ID
    private String jobMethod;//作业调度方法
    private String params;//作业参数
}
