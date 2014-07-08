package jp.co.gsol.oss.notifications;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

public class WebSocketManager {

    static public List<String> registeredProtocol() {
        return new ArrayList<>();
    }
    static public Optional<WebSocketExecutor> getProtocolsExecutor(final String protocol) {
        return Optional.absent();
    }
}
