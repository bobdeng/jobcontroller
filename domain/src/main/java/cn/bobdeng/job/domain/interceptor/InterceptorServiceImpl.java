package cn.bobdeng.job.domain.interceptor;

import cn.bobdeng.job.domain.entity.StepInterceptor;
import lombok.Data;

import java.util.List;

@Data
public class InterceptorServiceImpl implements InterceptorService {
    private InterceptorDAO interceptorDAO;

    @Override
    public List<StepInterceptor> getStepInterceptors() {
        return interceptorDAO.getStepInterceptors();
    }

    @Override
    public void addStepInterceptor(StepInterceptor interceptor) {
        interceptorDAO.addStepInterceptor(interceptor);
    }

    @Override
    public void removeStepInterceptor(String jobId, String stepId) {
        interceptorDAO.removeStepInterceptor(jobId, stepId);
    }
}
