package jp.co.gsol.oss.notifications.impl.contrib;

import java.io.IOException;

import jp.co.gsol.oss.notifications.impl.AbstractTakerImpl;
import jp.co.intra_mart.common.platform.log.Logger;
import com.caucho.websocket.WebSocketContext;
import com.google.common.base.Optional;

/**
 * broadcast protocol's WebSocket event handler.
 * @author Global solutions company limited
 */
public class BroadcastTakerImpl extends AbstractTakerImpl {

    @Override
    public Optional<String> processClass() {
        return Optional.of(BroadcastTask.class.getCanonicalName());
    }

    @Override
    public void onReadText(final WebSocketContext context,
            final String key, final String message) throws IOException {
        BroadcastManager.schedule(key, message);
        Logger.getLogger().debug(message);
    }

}
