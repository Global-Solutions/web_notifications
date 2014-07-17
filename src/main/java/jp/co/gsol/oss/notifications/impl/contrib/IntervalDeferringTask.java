package jp.co.gsol.oss.notifications.impl.contrib;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.base.Optional;

import jp.co.gsol.oss.notifications.impl.AbstractDeferringTask;
import jp.co.intra_mart.common.platform.log.Logger;

public class IntervalDeferringTask extends AbstractDeferringTask {

    @Override
    protected Future<Optional<Boolean>> signal(final String key, final Map<String, String> param,
            final int deferredCount, final ExecutorService es) {
        final String interval = param.get("interval");
        return es.submit(new Callable<Optional<Boolean>>() {
            @Override
            public Optional<Boolean> call() {
                try {
                    Thread.sleep(interval != null ? Long.valueOf(interval) : 10_000L);
                } catch (InterruptedException e) {
                    Logger.getLogger().debug("intrrupted", e);
                    return Optional.absent();
                }
                return Optional.of(true); //emit continue signal
            }
        });
    }
}
