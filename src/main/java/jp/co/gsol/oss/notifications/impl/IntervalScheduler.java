package jp.co.gsol.oss.notifications.impl;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import jp.co.intra_mart.common.platform.log.Logger;
import jp.co.intra_mart.foundation.asynchronous.TaskControlException;
import jp.co.intra_mart.foundation.asynchronous.TaskManager;

public class IntervalScheduler {
    private static class IntervalSchedulerHolder {
        private static final IntervalScheduler instance = new IntervalScheduler();
    }
    
    private static class Task {
        String className;
        Map<String, Object> param;
        Task(String className, Map<String, Object> param) {
            this.className = className;
            this.param = param;
        }
    }
    
    private Queue<Task> taskQueue = new ArrayDeque<>();
    
    private IntervalScheduler() {}
    
    public static IntervalScheduler getInstance() {
        return IntervalSchedulerHolder.instance;
    }
    
    public void add(String className, Map<String, Object> param) {
        synchronized (this.taskQueue) {
            if (this.taskQueue.isEmpty()) {
                try {
                    TaskManager.addParallelizedTask(IntervalTask.class.getName(), null);
                } catch (TaskControlException e) {
                    Logger.getLogger().error("task scheduler error", e);
                }
            }
            this.taskQueue.add(new Task(className, param));
        }
    }
    
    public void run() throws TaskControlException {
        while (!this.taskQueue.isEmpty()) {
            Task task = this.taskQueue.poll();
            TaskManager.addParallelizedTask(task.className, task.param);
        }
    }

}
