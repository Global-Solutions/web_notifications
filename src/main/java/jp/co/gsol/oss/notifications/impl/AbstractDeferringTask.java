package jp.co.gsol.oss.notifications.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import com.google.common.base.Optional;

import jp.co.intra_mart.common.platform.log.Logger;
import jp.co.intra_mart.foundation.asynchronous.AbstractTask;

/**
 * waiting task implements.
 * @author Global Solutions company limited
 */
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
        final Map<String, Object> nextParam = new HashMap<>(param);
        try {
            final Optional<Boolean> advance =
                    signal(key, signalParam,
                            count, ForkJoinCommonPool.COMMON_POOL).get();
            if (advance.isPresent()) {
                String advanceClass = deferredClass;
                if (advance.get()) {
                    nextParam.put("deferredCount", String.valueOf(0));
                } else {
                    nextParam.put("deferredCount", String.valueOf(count + 1));
                    advanceClass = this.getClass().getCanonicalName();
                }
                Logger.getLogger().debug("key: {}, count: {}, param: {}", key, count, signalParam);
                IntervalScheduler.getInstance().add(advanceClass, nextParam);
            }
        } catch (InterruptedException | ExecutionException e) {
            Logger.getLogger().error("event loop abort", e);
        }
    }
    /**
     * wait for firing event.
     * @param key identification for the session
     * @param param waiting configuration
     * @param deferredCount the times this task continued
     * @param fjp executor service
     * @return
     *      advance for the event process task -> true
     *      continue this waiting task -> false
     *      stop the event loop -> absent
     */
    protected abstract Future<Optional<Boolean>> signal(
            final String key, final Map<String, String> param,
            final int deferredCount,
            final ForkJoinPool fjp);
}