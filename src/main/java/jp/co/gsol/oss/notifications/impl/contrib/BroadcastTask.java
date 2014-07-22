package jp.co.gsol.oss.notifications.impl.contrib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON;

import jp.co.gsol.oss.notifications.impl.AbstractWebSocketTask;
import jp.co.intra_mart.foundation.cache.Cache;
import jp.co.intra_mart.foundation.cache.CacheManager;
import jp.co.intra_mart.foundation.cache.CacheManagerFactory;
import jp.co.intra_mart.foundation.context.Contexts;
import jp.co.intra_mart.foundation.context.model.AccountContext;

import com.google.common.base.Optional;

public class BroadcastTask extends AbstractWebSocketTask {

    private long processTime = 0;
    private long lastProcessTime = 0;
    @Override
    protected List<String> processedMessage(final String key,
            final Map<String, String> param) {
        final AccountContext ac = Contexts.get(AccountContext.class);
        final long now = System.currentTimeMillis();
        final String lastTimeStr = param.get("lastTime");
        final long lastTime = lastTimeStr != null ? Long.valueOf(lastTimeStr) : now;
        final CacheManager cm = CacheManagerFactory.getCacheManager();
        final Cache<Long, String> cache = cm.getCache("webNotificationsBroadcastCache");
        final List<String> messages = new ArrayList<>();
        for (Cache.Entry<Long, String> entry : cache)
            if (entry.getKey() > lastTime) {
                final Map<String, String> message = new HashMap<>();
                message.put("userCd", ac.getUserCd());
                message.put("value", entry.getValue());
                messages.add(JSON.encode(message));
            }
        processTime = now;
        lastProcessTime = lastTime != now ? lastTime : now;
        return messages;
    }

    @Override
    protected Map<String, String> done(final String key, final boolean sent) {
        final Map<String, String> param = new HashMap<>();
        if (processTime > 0) {
            if (sent || lastProcessTime == processTime)
                param.put("lastTime", String.valueOf(processTime));
            else if (lastProcessTime > 0)
                param.put("lastTime", String.valueOf(lastProcessTime));
        }
        return param;
    }

    @Override
    protected Optional<String> deferringTask() {
        return Optional.of(BroadcastDeferringTask.class.getCanonicalName());
    }
}
