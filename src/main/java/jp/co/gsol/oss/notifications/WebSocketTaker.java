package jp.co.gsol.oss.notifications;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.websocket.WebSocketContext;
import com.google.common.base.Optional;

/**
 * WebSocket event handling.
 * @author Global solutions company limited
 */
public interface WebSocketTaker {

    /**
     * canonical class name for processing push event loop.
     * @return class name
     */
    Optional<String> processClass();

    /**
     * session alive count.
     * @return count
     */
    int maxRequestCount();

    /**
     * register key's #{@link WebSocketContext}.
     * @param context key's WebSocketContext
     * @param key identification for the connection
     */
    void register(final WebSocketContext context, final String key);

    /**
     * start event handling.
     * @param context key's WebSocketContext
     * @param key identification for the connection
     */
    void onStart(final WebSocketContext context, final String key);


    /**
     * text message event handling.
     * @param context key's WebSocketContext
     * @param key identification for the connection
     * @param message received text message
     * @throws IOException maybe sendMessage error
     */
    void onReadText(final WebSocketContext context, final String key, String message) throws IOException;

    /**
     * binary message event handling.
     * @param context key's WebSocketContext
     * @param key identification for the connection
     * @param is received input stream
     * @throws IOException maybe sendMessage error
     */
    void onReadBinary(final WebSocketContext context, final String key, InputStream is) throws IOException;

    /**
     * close event handling.
     * @param context key's WebSocketContext
     * @param key identification for the connection
     */
    void onClose(final WebSocketContext context, final String key);

    /**
     * timeout event handling.
     * @param context key's WebSocketContext
     * @param key identification for the connection
     */
    void onTimeout(final WebSocketContext context, final String key);

    /**
     * disconnect event handling.
     * @param context key's WebSocketContext
     * @param key identification for the connection
     */
    void onDisconnect(final WebSocketContext context, final String key);

    /**
     * unregister some key's data.
     * @param context key's WebSocketContext
     * @param key identification for the connection
     */
    void unregister(final WebSocketContext context, final String key);
}
