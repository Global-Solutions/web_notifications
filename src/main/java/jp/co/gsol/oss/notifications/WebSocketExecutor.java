package jp.co.gsol.oss.notifications;

import java.io.InputStream;

import com.caucho.websocket.WebSocketContext;

public interface WebSocketExecutor {

    void register(WebSocketContext context);

    void onClose(WebSocketContext context);

    void onDisconnect(WebSocketContext context);

    void onTimeout(WebSocketContext context);

    void onReadBinary(WebSocketContext context, InputStream is);

    void onReadText(WebSocketContext context, String message);

    void unregister(WebSocketContext context);
}
