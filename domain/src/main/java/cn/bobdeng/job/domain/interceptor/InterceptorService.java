package cn.bobdeng.job.domain.interceptor;


import cn.bobdeng.job.domain.entity.StepInterceptor;

import java.util.List;

public interface InterceptorService {
    List<StepInterceptor> getStepInterceptors();

    void addStepInterceptor(StepInterceptor interceptor);

    void removeStepInterceptor(String jobId, String stepId);
}
