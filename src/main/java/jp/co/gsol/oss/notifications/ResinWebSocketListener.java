package jp.co.gsol.oss.notifications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import jp.co.intra_mart.common.platform.log.Logger;

import com.caucho.websocket.AbstractWebSocketListener;
import com.caucho.websocket.WebSocketContext;

/**
 * 
 *
 */
public class ResinWebSocketListener extends AbstractWebSocketListener {
    private final Logger logger = Logger.getLogger();

    private final WebSocketTaker taker;
    public final String key = String.valueOf(this.hashCode());
    public ResinWebSocketListener(final WebSocketTaker webSocketTaker) {
        // TODO 自動生成されたコンストラクター・スタブ
        taker = webSocketTaker;
        WebSocketContextPool.reserve(key);
    }

    @Override
    public final void onStart(final WebSocketContext context)
      throws IOException {
        // called when the connection starts
        taker.register(context, key);
        taker.onStart(context, key);
        WebSocketContextPool.registerContext(String.valueOf(this.hashCode()), context);
        logger.debug("start: {} timeout: {} key: {}", context.toString(), context.getTimeout(), key);
    }

    @Override
    public final void onReadText(final WebSocketContext context, final Reader is)
       {
        if (WebSocketContextPool.notRegistered(key)
         || WebSocketContextPool.zombieSession(key, taker.maxRequestCount()))
            context.close();
        try (final Reader reader = is; final BufferedReader br = new BufferedReader(reader)) {
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            final String message = sb.toString();
            taker.onReadText(context, key, message);
            logger.debug("readText: {}", context.toString());
        } catch (IOException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }
    }

    @Override
    public void onReadBinary(final WebSocketContext context, final InputStream is) {
        taker.onReadBinary(context, key, is);
        logger.debug("readBinary: {}", context.toString());
    }
    @Override
    public final void onClose(final WebSocketContext context) throws IOException {
    // called when the client closes gracefully
        taker.onClose(context, key);
        logger.debug("close: {}", context.toString());
    }
    @Override
    public void onTimeout(final WebSocketContext context) {
        taker.onTimeout(context, key);
        logger.debug("timeout: {}", context.toString());
    }
    @Override
    public final void onDisconnect(final WebSocketContext context) throws IOException {
    // called when the client closes disconnects
        taker.onDisconnect(context, key);
        taker.unregister(context, key);
        WebSocketContextPool.unregisterContext(key);
        logger.debug("disconnect: {}", context.toString());
    }
}

