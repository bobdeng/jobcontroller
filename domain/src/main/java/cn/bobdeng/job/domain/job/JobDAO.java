package cn.bobdeng.job.domain.job;


import cn.bobdeng.job.domain.entity.*;

import java.util.List;

public interface JobDAO {
    /**
     * 获取JOB配置
     * @param jobId
     * @return
     */
    JobConfig getJobConfig(String jobId);

    /**
     * 获取JOB配置里面的步骤
     * @param jobId
     * @return
     */
    List<JobStepConfig> getJobConfigSteps(String jobId);

    /**
     * 保存当前JOB，包括JOB和Step
     * @param job
     */
    void saveJob(Job job);

    /**
     * 用JOB加锁，分布式
     * @param jobId
     */
    void lock(String jobId);

    /**
     * 用JOB释放锁
     * @param jobId
     */
    void releaseLock(String jobId);

    /**
     * 保存JOB执行步骤
     * @param job
     */
    void saveSteps(Job job);

    /**
     * 获取JOB
     * @param jobId
     * @return
     */
    Job getJob(String jobId);


    /**
     * 获取所有正在执行的JOB
     * @return
     */
    List<Job> findAll();

    /**
     * 创建新JOB，不保存
     * @param jobId
     * @param params
     * @return
     */
    Job newJob(String jobId, String params);

    /**
     * 根据ID删除JOB
     * @param jobId
     */
    void deleteJobById(String jobId);

    /**
     * 获取所有JOB配置
     * @return
     */
    List<JobConfig> getAllJobConfig();

    /**
     * 刷新JOB配置
     * @param jobId
     */
    void reloadJobConfig(String jobId);

    /**
     * 放入一个Key，有过期时间
     * @param key
     * @param value
     * @param expireInMs
     */
    void putKey(String key, String value, int expireInMs);

    /**
     * 获取一个Key的Value
     * @param key
     * @return
     */
    String getKey(String key);
}
