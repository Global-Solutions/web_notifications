package jp.co.gsol.oss.notifications;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import com.caucho.websocket.WebSocketContext;
import com.google.common.base.Optional;

public class WebSocketContextPool {

    static final long waitingTimeout = 10_000;
    static final Map<String, WebSocketContext> pool = new ConcurrentHashMap<>();
    static final Map<String, Object> waitings = new ConcurrentHashMap<>();
    static final List<String> reserved = new CopyOnWriteArrayList<>();
    static final Map<String, Integer> aliving = new ConcurrentHashMap<>();
    static final long sweepInterval = 300_000;
    static long lastSweepTime = System.currentTimeMillis();

    static public void reserve(final String key) {
        reserved.add(key);
        aliving.put(key, 0);
    }
    static public void registerContext(final String key, final WebSocketContext context) {
        pool.put(key, context);
        reserved.remove(key);
        clearWaiting(key);
    }
    static public Optional<WebSocketContext> context(final String key) {
        return Optional.fromNullable(pool.get(key));
    }
    static public void unregisterContext(final String key) {
        pool.remove(key);
        reserved.remove(key);
        clearWaiting(key);
    }
    static void clearWaiting(final String key) {
        final Object waiting = waitings.get(key);
        if (waiting != null) {
            synchronized (waiting) {
                waiting.notify();
            }
            waitings.remove(key);
        }
    }
    public static Future<Optional<WebSocketContext>> futureGet(final String key, final ForkJoinPool fjp) {
        return fjp.submit(new Callable<Optional<WebSocketContext>>() {
            @Override
            public Optional<WebSocketContext> call() {
                if (notRegistered(key))
                    return Optional.absent();
                final Optional<WebSocketContext> context = context(key);
                if (context.isPresent())
                    return context;
                final Object lock = new Object();
                waitings.put(key, lock);
                synchronized (lock) {
                    while (waitings.containsKey(key))
                        try {
                            lock.wait(waitingTimeout);
                        } catch (final InterruptedException e) {
                            // TODO 自動生成された catch ブロック
                            e.printStackTrace();
                            return Optional.absent();
                        }
                }
                return context(key);
            }
        });
    }
    public static boolean notRegistered(final String key) {
        return !reserved.contains(key) && !pool.containsKey(key);
    }
    public static boolean sameSession(final String key) {
        if (aliving.containsKey(key)) {
            aliving.put(key, 0);
            return true;
        }

        return false;
    }
    public static void clearSession(final String key) {
        aliving.remove(key);
        sweepSession();
    }
    static synchronized void sweepSession() {
        final long now = System.currentTimeMillis();
        if (now - lastSweepTime > sweepInterval
         && aliving.size() > pool.size() + reserved.size()) {
            final Iterator<Map.Entry<String, Integer>> it = aliving.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, Integer> item = it.next();
                if (notRegistered(item.getKey()))
                    it.remove();
            }
            lastSweepTime = now;
        }
    }
    public static boolean zombieSession(final String key, final int maxCount) {
        final Integer count = aliving.get(key);
        if (count == null || count >= maxCount)
            return true;
        aliving.put(key, count + 1);
        return false;
    }
}
