package jp.co.gsol.oss.notifications;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import jp.co.intra_mart.common.platform.log.Logger;

import com.caucho.websocket.AbstractWebSocketListener;
import com.caucho.websocket.WebSocketContext;

/**
 * 
 *
 */
public class ResinWebSocketListener extends AbstractWebSocketListener {
    final Logger logger = Logger.getLogger();

    @Override
    public final void onStart(final WebSocketContext context)
      throws IOException {
      // called when the connection starts
      PrintWriter w = context.startTextMessage();
      w.println("connect");
      w.close();
      logger.debug("start: {} {}", context.toString(), context.getTimeout());
    }

    @Override
    public final void onReadText(final WebSocketContext context, final Reader is)
      throws IOException {
      final StringBuilder sb = new StringBuilder();
      int ch;
      while ((ch = is.read()) >= 0) {
        sb.append((char) ch);
      }
      final String message = sb.toString();
      logger.debug("message: {}", message);

      final PrintWriter out = context.startTextMessage();
      out.println(message);
      out.close();

      is.close();
    }

    //@Override
    //public void onReadBinary(final WebSocketContext context, final InputStream is) {
    //
    //}
    @Override
    public final void onClose(final WebSocketContext context) throws IOException {
    // called when the client closes gracefully
        logger.debug("close: {}", context.toString());
    }
    //@Override
    //public void onTimeout(final WebSocketContext context) {
    //
    //}
    @Override
    public final void onDisconnect(final WebSocketContext context) throws IOException {
    // called when the client closes disconnects
        logger.debug("disconnect: {}", context.toString());
    }
}
