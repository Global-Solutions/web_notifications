package jp.co.gsol.oss.notifications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;

import jp.co.intra_mart.common.platform.log.Logger;
import jp.co.intra_mart.foundation.context.model.AccountContext;

import com.caucho.websocket.AbstractWebSocketListener;
import com.caucho.websocket.WebSocketContext;

/**
 * 
 *
 */
public class ResinWebSocketListener extends AbstractWebSocketListener {
    final Logger logger = Logger.getLogger();

    final WebSocketTaker taker;
    public ResinWebSocketListener(final WebSocketTaker webSocketTaker,
            final AccountContext accountContext) {
        // TODO 自動生成されたコンストラクター・スタブ
        taker = webSocketTaker;
    }

    @Override
    public final void onStart(final WebSocketContext context)
      throws IOException {
        // called when the connection starts
        taker.register(context);
        final PrintWriter w = context.startTextMessage();
        w.println("connect");
        w.close();
        logger.debug("start: {} timeout: {}", context.toString(), context.getTimeout());
    }

    @Override
    public final void onReadText(final WebSocketContext context, final Reader is)
       {
        try (final Reader reader = is; final BufferedReader br = new BufferedReader(reader)) {
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            final String message = sb.toString();
            taker.onReadText(context, message);
            logger.debug("readText: {}", context.toString());
            final PrintWriter out = context.startTextMessage();
            out.println(message);
            out.close();
        } catch (IOException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }
    }

    @Override
    public void onReadBinary(final WebSocketContext context, final InputStream is) {
        taker.onReadBinary(context, is);
        logger.debug("readBinary: {}", context.toString());
    }
    @Override
    public final void onClose(final WebSocketContext context) throws IOException {
    // called when the client closes gracefully
        taker.onClose(context);
        logger.debug("close: {}", context.toString());
    }
    @Override
    public void onTimeout(final WebSocketContext context) {
        taker.onTimeout(context);
        logger.debug("timeout: {}", context.toString());
    }
    @Override
    public final void onDisconnect(final WebSocketContext context) throws IOException {
    // called when the client closes disconnects
        taker.onDisconnect(context);
        taker.unregister(context);
        logger.debug("disconnect: {}", context.toString());
    }
}

