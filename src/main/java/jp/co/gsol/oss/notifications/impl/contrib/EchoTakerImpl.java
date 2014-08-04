package jp.co.gsol.oss.notifications.impl.contrib;

import java.io.IOException;
import java.io.PrintWriter;

import jp.co.gsol.oss.notifications.impl.AbstractTakerImpl;
import jp.co.intra_mart.common.platform.log.Logger;

import com.caucho.websocket.WebSocketContext;

/**
 * echo protocol implements.
 * @author Global solutions company limited
 */
public class EchoTakerImpl extends AbstractTakerImpl {
    @Override
    public void onReadText(final WebSocketContext context,
            final String key, final String message) throws IOException {
        synchronized (context) {
            final PrintWriter out = context.startTextMessage();
            out.println(message);
            out.close();
        }
        Logger.getLogger().debug(message);
    }
}
