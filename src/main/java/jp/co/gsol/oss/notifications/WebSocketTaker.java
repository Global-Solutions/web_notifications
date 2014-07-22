package jp.co.gsol.oss.notifications;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.websocket.WebSocketContext;
import com.google.common.base.Optional;

public interface WebSocketTaker {

    Optional<String> processClass();

    int maxRequestCount();

    void register(final WebSocketContext context, final String key);

    void onStart(final WebSocketContext context, final String key);

    void onClose(final WebSocketContext context, final String key);

    void onDisconnect(final WebSocketContext context, final String key);

    void onTimeout(final WebSocketContext context, final String key);

    void onReadBinary(final WebSocketContext context, final String key, InputStream is);

    void onReadText(final WebSocketContext context, final String key, String message) throws IOException;

    void unregister(final WebSocketContext context, final String key);
}
