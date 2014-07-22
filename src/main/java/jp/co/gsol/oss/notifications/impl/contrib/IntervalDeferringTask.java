package jp.co.gsol.oss.notifications.impl.contrib;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.Future;

import com.google.common.base.Optional;

import jp.co.gsol.oss.notifications.impl.AbstractDeferringTask;
import jp.co.intra_mart.common.platform.log.Logger;

public class IntervalDeferringTask extends AbstractDeferringTask {
    private final static long unit = 10;

    @Override
    protected Future<Optional<Boolean>> signal(final String key, final Map<String, String> param,
            final int deferredCount, final ForkJoinPool fjp) {
        final String intervalStr = param.get("interval");
        final long interval = intervalStr != null ? Long.valueOf(intervalStr) : 10_000L;
        return fjp.submit(new Callable<Optional<Boolean>>() {
            @Override
            public Optional<Boolean> call() {
                try {
                    ForkJoinPool.managedBlock(new ManagedBlocker() {
                        private long elapsed = 0;
                        @Override
                        public boolean isReleasable() {
                            return elapsed >= interval;
                        }
                        @Override
                        public boolean block() throws InterruptedException {
                            if (!isReleasable()) {
                                Thread.sleep(unit);
                                elapsed += unit;
                            }
                            return isReleasable();
                        }
                    });
                } catch (InterruptedException e) {
                    Logger.getLogger().debug("intrrupted", e);
                    return Optional.absent();
                }
                //try {
                //    Thread.sleep(interval);
                //} catch (InterruptedException e) {
                //}
                return Optional.of(true); //emit continue signal
            }
        });
    }
}
