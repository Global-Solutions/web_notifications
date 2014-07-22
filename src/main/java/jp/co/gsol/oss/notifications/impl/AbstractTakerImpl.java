package jp.co.gsol.oss.notifications.impl;

import java.io.IOException;
import java.io.InputStream;

import jp.co.gsol.oss.notifications.WebSocketTaker;

import com.caucho.websocket.WebSocketContext;
import com.google.common.base.Optional;

public abstract class AbstractTakerImpl implements WebSocketTaker {

    @Override
    public void register(final WebSocketContext context, final String key) {
    }
    @Override
    public void onStart(final WebSocketContext context, final String key) {
    }
    @Override
    public void onClose(final WebSocketContext context, final String key) {
    }
    @Override
    public void onDisconnect(final WebSocketContext context, final String key) {
    }
    @Override
    public void onTimeout(final WebSocketContext context, final String key) {
    }
    @Override
    public void onReadBinary(final WebSocketContext context, final String key, final InputStream is) {
    }
    @Override
    public void onReadText(final WebSocketContext context, final String key, final String message) throws IOException {
    }
    @Override
    public void unregister(final WebSocketContext context, final String key) {
    }
    @Override
    public Optional<String> processClass() {
        return Optional.absent();
    }
    @Override
    public int maxRequestCount() {
        return 10;
    }
}
