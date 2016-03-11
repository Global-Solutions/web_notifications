package jp.co.gsol.oss.notifications.impl.contrib;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.Future;

import com.google.common.base.Optional;

import jp.co.gsol.oss.notifications.impl.AbstractDeferringTask;
import jp.co.intra_mart.common.platform.log.Logger;

/**
 * time based signal.
 * @author Global solutions company limited
 */
public class IntervalDeferringTask extends AbstractDeferringTask {
    /** unit time.*/
    private static final long UNIT = 10;
    /** default interval time.*/
    private static final long DEFAULT_INTERVAL = 10_000L;
    /** default repeat times.*/
    private static final int DEFAULT_REPEAT = 6;

    @Override
    protected Future<Optional<Boolean>> signal(final String key, final Map<String, String> param,
            final int deferredCount, final ForkJoinPool fjp) {
        final String intervalStr = param.get("interval");
        final String repeatStr = param.get("repeat");
        final long interval = intervalStr != null ? Long.valueOf(intervalStr) : DEFAULT_INTERVAL;
        final int repeat = repeatStr != null ? Integer.valueOf(repeatStr) : DEFAULT_REPEAT;
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
                                Thread.sleep(UNIT);
                                elapsed += UNIT;
                            }
                            return isReleasable();
                        }
                    });
                } catch (final InterruptedException e) {
                    Logger.getLogger().error("evant loop abort", e);
                    return Optional.absent();
                }
                return Optional.of(deferredCount + 1 >= repeat); //emit continue signal
            }
        });
    }
}
