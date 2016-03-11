package jp.co.gsol.oss.notifications.impl.contrib;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import com.google.common.base.Optional;

import jp.co.gsol.oss.notifications.impl.AbstractDeferringTask;
import jp.co.intra_mart.common.platform.log.Logger;
import jp.co.intra_mart.foundation.cache.Cache;
import jp.co.intra_mart.foundation.cache.CacheManager;
import jp.co.intra_mart.foundation.cache.CacheManagerFactory;


/**
 * when message is received, emit.
 * @author Global solutions company limited
 */
public class BroadcastDeferringTask extends AbstractDeferringTask {

    /** default value.*/
    private static final long DEFAULT_INTERVAL = 30_000L;

    @Override
    protected Future<Optional<Boolean>> signal(final String key, final Map<String, String> param,
            final int deferredCount, final ForkJoinPool fjp) {
        final String intervalStr = param.get("interval");
        final long interval = intervalStr != null ? Long.valueOf(intervalStr) : DEFAULT_INTERVAL;
        final CacheManager cm = CacheManagerFactory.getCacheManager();
        return fjp.submit(new Callable<Optional<Boolean>>() {
            @Override
            public Optional<Boolean> call() {
                if (!BroadcastManager.registeredMessage(key)) { // if key's messages not exist, wait 
                    final Object monitor = new Object();
                    synchronized (monitor) {
                        try {
                            BroadcastManager.registerSlot(key, monitor);
                            monitor.wait(interval);
                        } catch (final InterruptedException e) {
                            Logger.getLogger().error("event loop abort", e);
                            return Optional.absent();
                        } finally {
                            BroadcastManager.unregisterSlot(key);
                        }
                    }
                }

                final Cache<Long, String> cache = cm.getCache("webNotificationsBroadcastCache");
                for (String message : BroadcastManager.getAndRemoveMessages(key))
                    cache.put(System.currentTimeMillis(), message);
                return Optional.of(true);
            }
        });
    }

}
