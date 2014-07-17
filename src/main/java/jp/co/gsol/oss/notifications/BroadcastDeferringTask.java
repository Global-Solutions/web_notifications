package jp.co.gsol.oss.notifications;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.base.Optional;

import jp.co.intra_mart.foundation.cache.Cache;
import jp.co.intra_mart.foundation.cache.CacheManager;
import jp.co.intra_mart.foundation.cache.CacheManagerFactory;


public class BroadcastDeferringTask extends AbstractDeferringTask {

    @Override
    protected Future<Optional<Boolean>> signal(final String key, final Map<String, String> param,
            final int deferredCount, final ExecutorService es) {
        final CacheManager cm = CacheManagerFactory.getCacheManager();
        return es.submit(new Callable<Optional<Boolean>>() {
            @Override
            public Optional<Boolean> call() throws Exception {
                // TODO 自動生成されたメソッド・スタブ
                if (!BroadcastManager.registeredMessage(key)) {
                    final Object monitor = new Object();
                    synchronized (monitor) {
                        BroadcastManager.registerSlot(key, monitor);
                        monitor.wait(30_000L);
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
