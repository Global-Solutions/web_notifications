package jp.co.gsol.oss.notifications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import jp.co.intra_mart.common.platform.log.Logger;

import com.caucho.websocket.AbstractWebSocketListener;
import com.caucho.websocket.WebSocketContext;

/**
 * WebSocket listener for Resin.
 * @author Global solutions company limited
 */
public class ResinWebSocketListener extends AbstractWebSocketListener {
    /** logger.*/
    private final Logger logger = Logger.getLogger();
    /**
     * process event in practice.
     */
    private final WebSocketTaker taker;
    /**
     * identification for the connection.
     */
    public final String key = String.valueOf(this.hashCode());
    /**
     * prepare connection.
     * @param webSocketTaker inheritance of #{@link WebSocketTaker}
     */
    public ResinWebSocketListener(final WebSocketTaker webSocketTaker) {
        taker = webSocketTaker;
        WebSocketContextManager.reserve(key);
    }

    /**
     * register a context & dispatches a start event.
     * @param context #{@link WebSocketContext}
     */
    @Override
    public final void onStart(final WebSocketContext context) {
        // called when the connection starts
        WebSocketContextManager.registerContext(key, context);
        taker.register(context, key);
        taker.onStart(context, key);
        logger.debug("start: {} timeout: {} key: {}", context.toString(), context.getTimeout(), key);
    }

    /**
     * handles a text message & dispatches a readText event.
     * @param context #{@link WebSocketContext}
     * @param is #{@link Reader}
     */
    @Override
    public final void onReadText(final WebSocketContext context, final Reader is)
       {
        if (WebSocketContextManager.notRegistered(key)
         || WebSocketContextManager.zombieSession(key, taker.maxRequestCount())) {
            context.close();
            logger.info("session not found: {}, thus the connection closed", context.toString());
            return;
        }
        try (final Reader reader = is; final BufferedReader br = new BufferedReader(reader)) {
            final StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null)
                sb.append(line);

            final String message = sb.toString();
            taker.onReadText(context, key, message);
            logger.debug("readText: {}", context.toString());
        } catch (final IOException e) {
            logger.error("maybe send message error", e);
        }
    }

    /**
     * handles a binary message & dispatches a readBinary event.
     * @param context #{@link WebSocketContext}
     * @param is #{@link InputStream}
     */
    @Override
    public void onReadBinary(final WebSocketContext context, final InputStream is) {
        try {
            taker.onReadBinary(context, key, is);
            logger.debug("readBinary: {}", context.toString());
        } catch (final IOException e) {
            logger.error("maybe send message error", e);
        }
    }
    /**
     * dispatches a close event.
     * @param context #{@link WebSocketContext}
     */
    @Override
    public final void onClose(final WebSocketContext context) {
    // called when the client closes gracefully
        taker.onClose(context, key);
        logger.debug("close: {}", context.toString());
    }
    /**
     * dispatches a timeout event.
     * @param context #{@link WebSocketContext}
     */
    @Override
    public void onTimeout(final WebSocketContext context) {
        taker.onTimeout(context, key);
        logger.debug("timeout: {}", context.toString());
    }
    /**
     * dispatches a disconnect event & unregister the context.
     * @param context #{@link WebSocketContext}
     */
    @Override
    public final void onDisconnect(final WebSocketContext context) {
    // called when the client closes disconnects
        taker.onDisconnect(context, key);
        taker.unregister(context, key);
        WebSocketContextManager.unregisterContext(key);
        logger.debug("disconnect: {}", context.toString());
    }
}

