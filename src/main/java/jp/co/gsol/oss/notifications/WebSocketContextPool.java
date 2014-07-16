package jp.co.gsol.oss.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.caucho.websocket.WebSocketContext;
import com.google.common.base.Optional;

public class WebSocketContextPool {

    static final long waitingTimeout = 10_000;
    static final Map<String, WebSocketContext> pool = new HashMap<>();
    static final Map<String, Object> waitings = new HashMap<>();
    static final List<String> reserved = new ArrayList<>();
    static final List<String> aliving = new ArrayList<>();
    static final long sweepInterval = 300_000;
    static long lastSweepTime = System.currentTimeMillis();

    static public void reserve(final String key) {
        reserved.add(key);
        aliving.add(key);
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
    public static Future<Optional<WebSocketContext>> futureGet(final String key, final ExecutorService executor) {
        return executor.submit(new Callable<Optional<WebSocketContext>>() {
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
        return aliving.contains(key);
    }
    public static void clearSession(final String key) {
        aliving.remove(key);
        sweepSession();
    }
    static void sweepSession() {
        final long now = System.currentTimeMillis();
        if (now - lastSweepTime > sweepInterval
         && aliving.size() > pool.size() + reserved.size()) {
            final Iterator<String> it = aliving.iterator();
            while (it.hasNext())
                if (notRegistered(it.next()))
                    it.remove();
            lastSweepTime = now;
        }
    }
}
