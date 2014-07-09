package jp.co.gsol.oss.notifications;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import jp.co.intra_mart.common.platform.log.Logger;

import com.caucho.websocket.WebSocketContext;

public class EchoTakerImpl implements WebSocketTaker {

    @Override
    public void register(WebSocketContext context) {
        // TODO 自動生成されたメソッド・スタブ

    }

    @Override
    public void onClose(WebSocketContext context) {
        // TODO 自動生成されたメソッド・スタブ

    }

    @Override
    public void onDisconnect(WebSocketContext context) {
        // TODO 自動生成されたメソッド・スタブ

    }

    @Override
    public void onTimeout(WebSocketContext context) {
        // TODO 自動生成されたメソッド・スタブ

    }

    @Override
    public void onReadBinary(WebSocketContext context, InputStream is) {
        // TODO 自動生成されたメソッド・スタブ

    }

    @Override
    public void onReadText(WebSocketContext context, String message) throws IOException {
        // TODO 自動生成されたメソッド・スタブ
        final PrintWriter out = context.startTextMessage();
        out.println(message);
        out.close();
        Logger.getLogger().debug(message);
    }

    @Override
    public void unregister(WebSocketContext context) {
        // TODO 自動生成されたメソッド・スタブ

    }

}
