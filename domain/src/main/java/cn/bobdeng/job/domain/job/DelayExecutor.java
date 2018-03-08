package cn.bobdeng.job.domain.job;

public interface DelayExecutor {
    void execute(Runnable runnable, long delay);
}
