package jp.co.gsol.oss.notifications;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import jp.co.intra_mart.common.platform.log.Logger;

import com.caucho.websocket.WebSocketContext;
import com.google.common.base.Optional;

/**
 * management for #{@link WebSocketContext}.
 * @author Global solutions company limited
 */
public final class WebSocketContextManager {

    /** waiting monitor's timeout.*/
    private static final long WAITING_TIMEOUT = 10_000;
    /** key's WebSocketContext is stored.*/
    private static final Map<String, WebSocketContext> POOL = new ConcurrentHashMap<>();
    /** waiting Objects for key's WebSocketContext is registered.*/
    private static final Map<String, Object> WAITING = new ConcurrentHashMap<>();
    /** key's sessions are reserved.*/
    private static final List<String> RESERVED = new CopyOnWriteArrayList<>();
    /** alive sessions.*/
    private static final Map<String, Integer> ALIVES = new ConcurrentHashMap<>();
    /** sweeping session interval.*/
    private static final long SWEEP_INTERVAL = 300_000;
    /** previous sweeping session time.*/
    private static long lastSweepTime = System.currentTimeMillis();
    /** .*/
    private WebSocketContextManager() { }
    /**
     * reserve new session.
     * @param key identification for the connection
     */
    public static void reserve(final String key) {
        RESERVED.add(key);
        ALIVES.put(key, 0);
    }
    /**
     * register #{@link WebSocketContext} to key's session.
     * @param key identification for connection
     * @param context Resin's WebSocketContext
     */
    public static void registerContext(final String key, final WebSocketContext context) {
        POOL.put(key, context);
        RESERVED.remove(key);
        clearWaiting(key);
    }
    /**
     * get registered #{@link WebSocketContext}.
     * @param key identification for the connection
     * @return key's WebSocketContext
     */
    public static Optional<WebSocketContext> context(final String key) {
        return Optional.fromNullable(POOL.get(key));
    }
    /**
     * unregister key's session.
     * @param key identification for the connection
     */
    public static void unregisterContext(final String key) {
        POOL.remove(key);
        RESERVED.remove(key);
        clearWaiting(key);
    }
    /**
     * clear monitor object for key's session.
     * @param key identification for the session
     */
    private static void clearWaiting(final String key) {
        final Object waiting = WAITING.get(key);
        if (waiting != null) {
            synchronized (waiting) {
                waiting.notify();
            }
            WAITING.remove(key);
        }
    }
    /**
     * fetch key's #{@link WebSocketContext} asynchronously.
     * @param key identification for the session
     * @param fjp executor service
     * @return key's WebSocketContext
     */
    public static Future<Optional<WebSocketContext>> futureGet(final String key, final ForkJoinPool fjp) {
        return fjp.submit(new Callable<Optional<WebSocketContext>>() {
            @Override
            public Optional<WebSocketContext> call() {
                if (notRegistered(key))
                    return Optional.absent();
                final Optional<WebSocketContext> context = context(key);
                if (context.isPresent()) // already registered
                    return context;
                final Object lock = new Object();
                WAITING.put(key, lock);
                synchronized (lock) {
                    while (WAITING.containsKey(key))
                        try {
                            lock.wait(WAITING_TIMEOUT); // wait until context is registered
                        } catch (final InterruptedException e) {
                            Logger.getLogger().error("abort the session", e);
                            return Optional.absent();
                        }
                }
                return context(key);
            }
        });
    }
    /**
     * if key's session is not registered or reserved, true.
     * @param key identification for the session
     * @return if not registered, true
     */
    public static boolean notRegistered(final String key) {
        return !RESERVED.contains(key) && !POOL.containsKey(key);
    }
    /**
     * if key's session exist on the server, true.
     * @param key identification for the session
     * @return if key's session exist, true
     */
    public static boolean sameSession(final String key) {
        if (ALIVES.containsKey(key)) {
            ALIVES.put(key, 0);
            return true;
        }

        return false;
    }
    /**
     * clear key's session.
     * @param key identification for the session
     */
    public static void clearSession(final String key) {
        ALIVES.remove(key);
        sweepSession();
    }
    /**
     * if any sessions are not registered, sweep them as certain intervals.
     */
    private static synchronized void sweepSession() {
        final long now = System.currentTimeMillis();
        if (now - lastSweepTime > SWEEP_INTERVAL
         && ALIVES.size() > POOL.size() + RESERVED.size()) {
            final Iterator<Map.Entry<String, Integer>> it = ALIVES.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, Integer> item = it.next();
                if (notRegistered(item.getKey()))
                    it.remove();
            }
            lastSweepTime = now;
        }
    }
    /**
     * if key's session is not updated for a long time, true.
     * @param key identification for the session
     * @param maxCount the number of checking session
     * @return if key's session is a zombie, true
     */
    public static boolean zombieSession(final String key, final int maxCount) {
        final Integer count = ALIVES.get(key);
        if (count == null || count >= maxCount)
            return true;
        ALIVES.put(key, count + 1);
        return false;
    }
}
