package jp.co.gsol.oss.notifications.impl.contrib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * management for broadcast messages.
 * @author Global solutions company limited
 */
public final class BroadcastManager {

    /** .*/
    private BroadcastManager() { }
    /** wait for messages that are registered.*/
    private static final Map<String, Object> MONITORS = new ConcurrentHashMap<>();

    /** messages to be broadcast.*/
    private static final List<Map<String, String>> MESSAGES = new CopyOnWriteArrayList<>();
    /** messages waiting for monitor.*/
    private static final List<Map<String, String>> WAITING_MESSAGES = new CopyOnWriteArrayList<>();

    /**
     * register receiving event monitor.
     * @param key identification for the session
     * @param monitor monitor object for receiving message
     */
    public static void registerSlot(final String key, final Object monitor) {
        MONITORS.put(key, monitor);
    }
    /**
     * unregister key's monitor.
     * @param key identification for the session
     */
    public static void unregisterSlot(final String key) {
        MONITORS.remove(key);
    }
    /**
     * wake up all monitors.
     */
    public static void wakeupAll() {
        for (Map.Entry<String, Object> monitor : MONITORS.entrySet()) {
            final Object m = monitor.getValue();
            synchronized (m) {
                m.notify();
            }
        }
    }
    /**
     * schedule a broadcast message.
     * @param key identification for the session
     * @param message string to broadcast
     */
    public static void schedule(final String key, final String message) {
        final Object monitor = MONITORS.get(key);
        final Map<String, String> item = new HashMap<>();
        item.put("key", key);
        item.put("message", message);
        if (monitor != null) { // if key's monitor exist, notify
            synchronized (monitor) {
                MESSAGES.add(item);
                monitor.notify();
            }
        } else { // if not, wait for registered
            item.put("time", String.valueOf(System.currentTimeMillis()));
            WAITING_MESSAGES.add(item);
        }
    }
    /**
     * filter & remove key's messages.
     * @param key identification for the session
     * @return messages string list
     */
    public static List<String> getAndRemoveMessages(final String key) {
        return getAndRemoveMessages(key, MESSAGES);
    }
    /**
     * if key's messages have already registered, true.
     * @param key identification for the session
     * @return key's messages existent
     */
    public static boolean registeredMessage(final String key) {
        final List<Map<String, String>> wm = getAndRemoveItems(key, WAITING_MESSAGES);
        MESSAGES.addAll(wm);
        return !wm.isEmpty();
    }
    /**
     * filter & remove key's messages.
     * @param key identification for the session
     * @param targetMessages messages to be processed
     * @return message hash list
     */
    private static List<Map<String, String>> getAndRemoveItems(
            final String key, final List<Map<String, String>> targetMessages) {
        final List<Map<String, String>> items = new ArrayList<>();
        for (Map<String, String> item : targetMessages)
            if (key.equals(item.get("key")))
                items.add(item);
        targetMessages.removeAll(items);
        return items;
    }
    /**
     * filter & remove key's messages.
     * @param key identification for the session
     * @param targetMessages messages to be processed
     * @return message string list
     */
    private static List<String> getAndRemoveMessages(final String key, final List<Map<String, String>> targetMessages) {
        final List<String> keysMessages = new ArrayList<>();
        for (Map<String, String> item : getAndRemoveItems(key, targetMessages)) {
            keysMessages.add(item.get("message"));
        }
        return keysMessages;
    }
}
