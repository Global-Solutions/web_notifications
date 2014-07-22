package jp.co.gsol.oss.notifications.impl.contrib;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import com.google.common.base.Optional;

import jp.co.gsol.oss.notifications.impl.AbstractDeferringTask;
import jp.co.intra_mart.foundation.cache.Cache;
import jp.co.intra_mart.foundation.cache.CacheManager;
import jp.co.intra_mart.foundation.cache.CacheManagerFactory;


public class BroadcastDeferringTask extends AbstractDeferringTask {

    @Override
    protected Future<Optional<Boolean>> signal(final String key, final Map<String, String> param,
            final int deferredCount, final ForkJoinPool fjp) {
        final String intervalStr = param.get("interval");
        final long interval = intervalStr != null ? Long.valueOf(intervalStr) : 30_000L;
        final CacheManager cm = CacheManagerFactory.getCacheManager();
        return fjp.submit(new Callable<Optional<Boolean>>() {
            @Override
            public Optional<Boolean> call() throws Exception {
                // TODO 自動生成されたメソッド・スタブ
                if (!BroadcastManager.registeredMessage(key)) {
                    final Object monitor = new Object();
                    synchronized (monitor) {
                        BroadcastManager.registerSlot(key, monitor);
                        monitor.wait(interval);
                        BroadcastManager.unregisterSlot(key);
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
