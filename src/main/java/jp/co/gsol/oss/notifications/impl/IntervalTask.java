package jp.co.gsol.oss.notifications.impl;

import java.util.Map;

import jp.co.intra_mart.common.platform.log.Logger;
import jp.co.intra_mart.foundation.asynchronous.Task;
import jp.co.intra_mart.foundation.asynchronous.TaskEvent;

public class IntervalTask implements Task {
    
    public IntervalTask() {}
    
    private static final long interval = 10_000L;

    @Override
    public void run() {
        try {
            Thread.sleep(interval);
            IntervalScheduler.getInstance().run();
        } catch (InterruptedException e) {
            Logger.getLogger().error("task scheduler error", e);
        }
    }

    @Override
    public void release() {
    }
    @Override
    public void setParameter(Map<String, ?> arg0) {
    }
    @Override
    public void taskAccepted(TaskEvent arg0) {
    }
    @Override
    public void taskCompleted(TaskEvent arg0) {
    }
    @Override
    public void taskRejected(TaskEvent arg0) {
    }
    @Override
    public void taskStarted(TaskEvent arg0) {
    }

}
