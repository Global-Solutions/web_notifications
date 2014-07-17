package jp.co.gsol.oss.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BroadcastManager {

    private static final Map<String, Object> monitors = new ConcurrentHashMap<>();

    private static final List<Map<String, String>> messages = new CopyOnWriteArrayList<>();
    private static final List<Map<String, String>> waitingMessages = new CopyOnWriteArrayList<>();

    static void registerSlot(final String key, final Object monitor) {
        monitors.put(key, monitor);
    }
    static void unregisterSlot(final String key) {
        monitors.remove(key);
    }
    static void wakeupAll() {
        for (Map.Entry<String, Object> monitor : monitors.entrySet()) {
            final Object m = monitor.getValue();
            synchronized (m) {
                m.notify();
            }
        }
    }
    static void schedule(final String key, final String message) {
        final Object monitor = monitors.get(key);
        final Map<String, String> item = new HashMap<>();
        item.put("key", key);
        item.put("message", message);
        if (monitor != null) {
            synchronized (monitor) {
                messages.add(item);
                monitor.notify();
            }
        } else {
            item.put("time", String.valueOf(System.currentTimeMillis()));
            waitingMessages.add(item);
        }
    }
    static List<String> getAndRemoveMessages(final String key) {
        return getAndRemoveMessages(key, messages);
    }
    static boolean registeredMessage(final String key) {
        final List<Map<String, String>> wm = getAndRemoveItems(key, waitingMessages);
        messages.addAll(wm);
        return !wm.isEmpty();
    }
    private static List<Map<String, String>> getAndRemoveItems(
            final String key, final List<Map<String, String>> targetMessages) {
        final List<Map<String, String>> items = new ArrayList<>();
        for (Map<String, String> item : targetMessages)
            if (key.equals(item.get("key")))
                items.add(item);
        targetMessages.removeAll(items);
        return items;
    }
    private static List<String> getAndRemoveMessages(final String key, final List<Map<String, String>> targetMessages) {
        final List<String> keysMessages = new ArrayList<>();
        for (Map<String, String> item : getAndRemoveItems(key, targetMessages)) {
            keysMessages.add(item.get("message"));
        }
        return keysMessages;
    }
}
