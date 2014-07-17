package jp.co.gsol.oss.notifications;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.base.Optional;

import jp.co.intra_mart.foundation.asynchronous.AbstractTask;
import jp.co.intra_mart.foundation.asynchronous.TaskControlException;
import jp.co.intra_mart.foundation.asynchronous.TaskManager;

public abstract class AbstractDeferringTask extends AbstractTask {
    @Override
    public final void run() {
        final Map<String, ?> param = getParameter();
        final String key = (String) param.get("key");
        final String deferredClass = (String) param.get("deferredClass");
        final Object deferringParam = param.get("deferringParam");
        final Map<String, String> signalParam = deferringParam instanceof Map<?, ?>
            ? (Map<String, String>) deferringParam : new HashMap<String, String>();
        final String deferredCount = (String) param.get("deferredCount");
        final int count = deferredCount != null ? Integer.valueOf(deferredCount) : 0;
        final ExecutorService es = Executors.newSingleThreadExecutor();
        final Map<String, Object> nextParam = new HashMap<>(param);
        try {
            final Optional<Boolean> advance =
                    signal(key, signalParam,
                            count, es).get();
            if (advance.isPresent()) {
                if (advance.get()) {
                    nextParam.put("deferredCount", String.valueOf(0));
                    TaskManager.addParallelizedTask(deferredClass, nextParam);
                } else {
                    nextParam.put("deferredCount", String.valueOf(count + 1));
                    TaskManager.addParallelizedTask(this.getClass().getCanonicalName(), nextParam);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        } catch (TaskControlException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }
    }
    abstract protected Future<Optional<Boolean>> signal(
            final String key, final Map<String, String> param,
            final int deferredCount,
            final ExecutorService es);
}