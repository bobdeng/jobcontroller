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
@NoArgsConstructor
@AllArgsConstructor
public class JobResult {
    private String jobSerialId;//作业唯一ID
    private String jobId;//作业ID
    private String jobStepId;//作业步骤ID
    private String jobMethod;//作业调度方法
    private boolean success;//是否成功
    private String output;//返回的结果
}
