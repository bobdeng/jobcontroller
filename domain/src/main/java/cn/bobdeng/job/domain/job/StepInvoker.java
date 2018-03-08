package cn.bobdeng.job.domain.job;

import cn.bobdeng.job.domain.entity.JobMethod;

public interface StepInvoker {
    void invoke(JobMethod stepMethod);

    void invokeJobMethodViaHttp(String host, JobMethod jobMethod);

}
