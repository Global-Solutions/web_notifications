package jp.co.gsol.oss.notifications;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jp.co.intra_mart.foundation.asynchronous.AbstractTask;
import jp.co.intra_mart.foundation.asynchronous.TaskControlException;
import jp.co.intra_mart.foundation.asynchronous.TaskManager;

public class AbstractDeferringTask extends AbstractTask {
    @Override
    public void run() {
        final Map<String, ?> param = getParameter();
        final String key = (String) param.get("key");
        final String deferredClass = (String) param.get("deferredClass");
        final Object deferringParam = param.get("deferringParam");
        final Map<String, String> signalParam = deferringParam instanceof Map<?, ?>
            ? (Map<String, String>) deferringParam : new HashMap<String, String>();
        final ExecutorService es = Executors.newSingleThreadExecutor();
        final Map<String, Object> nextParam = new HashMap<>(param);
        try {
            if (signal(key, signalParam, es).get())
                TaskManager.addParallelizedTask(deferredClass, nextParam);
        } catch (InterruptedException | ExecutionException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        } catch (TaskControlException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }
    }
    protected Future<Boolean> signal(final String key, final Map<String, String> param,
            final ExecutorService es) {
        final String interval = param.get("interval");
        return es.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    Thread.sleep(interval != null ? Long.valueOf(interval) : 10_000);
                } catch (InterruptedException e) {
                    // TODO 自動生成された catch ブロック
                    e.printStackTrace();
                }
                return true;
            }
        });
    }
}