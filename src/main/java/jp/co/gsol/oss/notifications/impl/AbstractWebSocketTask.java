package jp.co.gsol.oss.notifications.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.caucho.websocket.WebSocketContext;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import jp.co.gsol.oss.notifications.WebSocketContextManager;
import jp.co.gsol.oss.notifications.impl.contrib.IntervalDeferringTask;
import jp.co.intra_mart.common.platform.log.Logger;
import jp.co.intra_mart.foundation.asynchronous.AbstractTask;
import jp.co.intra_mart.foundation.asynchronous.TaskControlException;
import jp.co.intra_mart.foundation.asynchronous.TaskManager;

/**
 * push event loop & default configuration.
 * @author Global Solutions company limited
 */
public abstract class AbstractWebSocketTask extends AbstractTask {
    /** waiting unit time.*/
    protected static final int DEFERRING_INTERVAL = 10_000;
    /** repeat times for a cycle.*/
    protected static final int DEFERRING_REPEAT = 6;
    /** default propagation parameter for waiting task.*/
    protected static final Map<String, String> DEFERRING_INTERVAL_PARAM =
            ImmutableMap
            .of("interval", String.valueOf(DEFERRING_INTERVAL),
                "repeat", String.valueOf(DEFERRING_REPEAT));
    /** the number of retrying to find session.*/
    protected static final int RETRY_LIMIT = 10;
    /** deceleration for retry to find session.*/
    protected static final int RETRY_DECELERATION = 1_000;
    @Override
    public final void run() {
        final Map<String, ?> param = getParameter();
        final String key = (String) param.get("key");
        final Map<String, Object> nextParam = new HashMap<>(param);
        // if not same ContextPool, retry
        if (!WebSocketContextManager.sameSession(key))
            try {
                final String retryCount = (String) param.get("retryCount");
                final int count = retryCount != null ? Integer.valueOf(retryCount) : 0;
                final Optional<Map<String, String>> retryParam = retryParam(key, count);
                if (retryParam.isPresent()) {
                    nextParam.put("deferringParam", retryParam.get());
                    nextParam.put("deferredClass", this.getClass().getCanonicalName());
                    nextParam.put("retryCount", String.valueOf(count + 1));
                    TaskManager.addParallelizedTask(IntervalDeferringTask.class.getCanonicalName(), nextParam);
                }
            } catch (final TaskControlException e) {
                Logger.getLogger().error("event loop abort", e);
            }
        else
            nextParam.put("retryCount", String.valueOf(0));

        Optional<WebSocketContext> context = Optional.absent();
        try {
            // wait for fetching context
            context = WebSocketContextManager.futureGet(key, ForkJoinCommonPool.COMMON_POOL).get();
        } catch (final ExecutionException | InterruptedException e) {
            Logger.getLogger().error("fetching context error", e);
        }

        // if key's context is absent, end event loop
        if (!context.isPresent()) {
            WebSocketContextManager.clearSession(key);
            return;
        }

        final Object lastObject = param.get("lastParam");
        final Map<String, String> lastParam = lastObject instanceof Map<?, ?>
                                            ? (Map<String, String>) lastObject : null;

        final List<String> processed = processedMessage(key,
                lastParam != null && !lastParam.isEmpty() ? lastParam : initialParam(key));
        if ((context = WebSocketContextManager.context(key)).isPresent()) {
                nextParam.put("lastParam", done(key, sendMessage(processed, context.get())));
            try {
                final Optional<String> deferringTask = deferringTask();
                // continue loop
                if (deferringTask.isPresent()) {
                    nextParam.put("deferredClass", this.getClass().getCanonicalName());
                    nextParam.put("deferringParam", deferringParam(key));
                    TaskManager.addParallelizedTask(deferringTask.get(), nextParam);
                }
            } catch (final TaskControlException e) {
                Logger.getLogger().error("event loop abort", e);
            }
        } else
            WebSocketContextManager.clearSession(key);
    }
    /**
     * send messages to the context's client.
     * @param messages some text messages
     * @param context the session's #{@link WebSocketContext}
     * @return if any messages are sent, true
     */
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
        } catch (final IOException e) {
            Logger.getLogger().error("send message error", e);
            return false;
        }
        return sent;
    }

    /**
     * prepare messages to send.
     * @param key identification of the session
     * @param param propagation parameter from previous event.
     * @return sent messages
     */
    protected abstract List<String> processedMessage(final String key, final Map<String, String> param);
    /**
     * post process for sending message & prepare next parameter.
     * @param key identification of the session
     * @param sent if any messages is sent, true
     * @return propagation parameter for next event
     */
    protected abstract Map<String, String> done(final String key, final boolean sent);

    /**
     * waiting task.
     * @return canonical class name
     */
    protected Optional<String> deferringTask() {
        return Optional.of(IntervalDeferringTask.class.getCanonicalName());
    }
    /**
     * propagation parameter for first time.
     * @param key identification for the session
     * @return propagation parameter
     */
    protected Map<String, String> initialParam(final String key) {
        return new HashMap<>();
    }
    /**
     * propagation parameter for waiting task.
     * @param key identification for the session
     * @return propagation parameter
     */
    protected Map<String, String> deferringParam(final String key) {
        return DEFERRING_INTERVAL_PARAM;
    }
    /**
     * propagation parameter for finding no session.
     * @param key identification for the session
     * @param count the number of retrying to find session
     * @return propagation parameter for next trying
     */
    protected Optional<Map<String, String>> retryParam(final String key, final int count) {
        if (count >= RETRY_LIMIT)
            return Optional.absent();
        final Map<String, String> param = new HashMap<>();
        param.put("interval", String.valueOf(RETRY_DECELERATION * count));
        param.put("repeat", String.valueOf(1));
        return Optional.of(param);
    }
}
