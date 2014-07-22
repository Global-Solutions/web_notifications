package jp.co.gsol.oss.notifications.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.caucho.websocket.WebSocketContext;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import jp.co.gsol.oss.notifications.WebSocketContextPool;
import jp.co.gsol.oss.notifications.impl.contrib.IntervalDeferringTask;
import jp.co.intra_mart.foundation.asynchronous.AbstractTask;
import jp.co.intra_mart.foundation.asynchronous.TaskControlException;
import jp.co.intra_mart.foundation.asynchronous.TaskManager;
import jp.co.intra_mart.foundation.asynchronous.report.RegisteredParallelizedTaskInfo;

public abstract class AbstractWebSocketTask extends AbstractTask {
    protected static final int deferringInterval = 30_000;
    protected static final Map<String, String> deferringIntervalParam =
            ImmutableMap.of("interval", String.valueOf(deferringInterval));
    protected static final int retryLimit = 10;
    protected static final int retryDeceleation = 1_000;
    @Override
    public final void run() {
        final Map<String, ?> param = getParameter();
        final String key = (String) param.get("key");
        final Map<String, Object> nextParam = new HashMap<>(param);
        // if not same ContextPool, retry
        if (!WebSocketContextPool.sameSession(key))
            try {
                final String retryCount = (String) param.get("retryCount");
                final int count = retryCount != null ? Integer.valueOf(retryCount) : 0;
                Optional<Map<String, String>> retryParam = retryParam(key, count);
                if (retryParam.isPresent()) {
                    nextParam.put("deferringParam", retryParam.get());
                    nextParam.put("deferredClass", this.getClass().getCanonicalName());
                    nextParam.put("retryCount", String.valueOf(count + 1));
                    TaskManager.addParallelizedTask(IntervalDeferringTask.class.getCanonicalName(), nextParam);
                }
            } catch (TaskControlException e1) {
                // TODO 自動生成された catch ブロック
                e1.printStackTrace();
            }
        else
            nextParam.put("retryCount", String.valueOf(0));

        Optional<WebSocketContext> context = Optional.absent();
        try {
            context = WebSocketContextPool.futureGet(key, ForkJoinCommonPool.commonPool).get();
        } catch (ExecutionException | InterruptedException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }

        if (!context.isPresent()) {
            WebSocketContextPool.clearSession(key);
            return;
        }

        final Object lastObject = param.get("lastParam");
        final Map<String, String> lastParam = lastObject instanceof Map<?, ?>
                                            ? (Map<String, String>) lastObject : initialParam(key);
        final List<String> processed = processedMessage(key, lastParam);
        if ((context = WebSocketContextPool.context(key)).isPresent()) {
            nextParam.put("lastParam", done(key, sendMessage(processed, context.get())));
            try {
                final Set<RegisteredParallelizedTaskInfo> running = TaskManager.getRegisteredInfo().getParallelizedTaskQueueInfo().getRunningTasksInfo();
                for (RegisteredParallelizedTaskInfo info : running)
                    System.out.println("k" + key + "runnning:" + info.getMessageId() + "@" + info.getNode());

                final Optional<String> deferringTask = deferringTask();
                if (deferringTask.isPresent()) {
                    nextParam.put("deferredClass", this.getClass().getCanonicalName());
                    nextParam.put("deferringParam", deferringParam(key));
                    TaskManager.addParallelizedTask(deferringTask.get(), nextParam);
                }
            } catch (TaskControlException e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        } else
            WebSocketContextPool.clearSession(key);
    }
    private boolean sendMessage(final List<String> messages, final WebSocketContext context) {
        boolean sent = false;
        try {
            synchronized (context) {
                for (String message : messages) {
                    final PrintWriter out = context.startTextMessage();
                    out.print(message);
                    out.close();
                    sent = true;
                }
            }
        } catch (IOException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
            return false;
        }
        return sent;
    }

    abstract protected List<String> processedMessage(final String key, final Map<String, String> param);
    abstract protected Map<String, String> done(final String key, final boolean sent);

    protected Optional<String> deferringTask() {
        return Optional.of(IntervalDeferringTask.class.getCanonicalName());
    }
    protected Map<String, String> initialParam(final String key) {
        return new HashMap<>();
    }
    protected Map<String, String> deferringParam(final String key) {
        return deferringIntervalParam;
    }
    protected Optional<Map<String, String>> retryParam(final String key, final int count) {
        if (count > retryLimit)
            return Optional.absent();
        final Map<String, String> param = new HashMap<>();
        param.put("interval", String.valueOf(retryDeceleation * count));
        return Optional.of(param);
    }
}
