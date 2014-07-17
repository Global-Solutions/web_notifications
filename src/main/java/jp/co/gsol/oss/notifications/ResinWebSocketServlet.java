package jp.co.gsol.oss.notifications;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.websocket.WebSocketListener;
import com.caucho.websocket.WebSocketServletRequest;
import com.google.common.base.Optional;

import jp.co.intra_mart.common.platform.log.Logger;
import jp.co.intra_mart.foundation.asynchronous.InvalidTaskException;
import jp.co.intra_mart.foundation.asynchronous.TaskControlException;
import jp.co.intra_mart.foundation.asynchronous.TaskIllegalStateException;
import jp.co.intra_mart.foundation.asynchronous.TaskManager;
import jp.co.intra_mart.foundation.context.Contexts;
import jp.co.intra_mart.foundation.context.model.AccountContext;
import jp.co.intra_mart.system.log.transition.TransitionLogHttpServletRequestWrapper;


/**
 * 
 *
 */
public class ResinWebSocketServlet extends GenericServlet {
    /** . */
    private static final long serialVersionUID = 3369924150095133613L;

    @Override
    public void service(final ServletRequest request, final ServletResponse response)
    throws IOException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse res = (HttpServletResponse) response;

        final String protocol = req.getHeader("Sec-WebSocket-Protocol");
        final Optional<WebSocketTaker> taker = WebSocketTakerManager.getProtocolsTaker(protocol);
        if (taker.isPresent()) {
            final AccountContext ac = Contexts.get(AccountContext.class);
            final WebSocketTaker wst = taker.get();
            final WebSocketListener listener = new ResinWebSocketListener(wst, ac);
            res.setHeader("Sec-WebSocket-Protocol", protocol);
            final WebSocketServletRequest wsRequest = (WebSocketServletRequest)
                    ((TransitionLogHttpServletRequestWrapper) request).getRequest();
            wsRequest.startWebSocket(listener);
            if (wst.processClass().isPresent()) {
                try {
                    final Map<String, Object> param = new HashMap<>();
                    param.put("key", String.valueOf(listener.hashCode()));
                    TaskManager.addParallelizedTask(wst.processClass().get(), param);
                } catch (TaskControlException e) {
                    // TODO 自動生成された catch ブロック
                    e.printStackTrace();
                }
            }
        } else {
            Logger.getLogger().debug("invalid protocol: {}", protocol);
            res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }
    }

}