package jp.co.gsol.oss.notifications;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.caucho.websocket.WebSocketServletRequest;
import com.google.common.base.Optional;

import jp.co.gsol.oss.notifications.impl.IntervalScheduler;
import jp.co.intra_mart.common.platform.log.Logger;
import jp.co.intra_mart.foundation.context.Contexts;
import jp.co.intra_mart.foundation.context.model.AccountContext;
import jp.co.intra_mart.system.log.transition.TransitionLogHttpServletRequestWrapper;


/**
 * WebSocketServlet for resin.
 * @author Global solutions company limited
 */
public class ResinWebSocketServlet extends GenericServlet {
    /** . */
    private static final long serialVersionUID = 3369924150095133613L;

    /**
     * Receives request & dispatches #{@link WebSocketTaker} to #{@link ResinWebSocketListener}.
     * @param request #{@link ServletRequest}
     * @param response #{@link ServletResponse}
     * @throws IOException #{@link WebSocketServletRequest#startWebSocket(com.caucho.websocket.WebSocketListener)}
     */
    @Override
    public void service(final ServletRequest request, final ServletResponse response)
    throws IOException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse res = (HttpServletResponse) response;
        final String userCd = Contexts.get(AccountContext.class).getUserCd();

        final String protocol = req.getHeader("Sec-WebSocket-Protocol");
        final Optional<WebSocketTaker> taker = WebSocketTakerManager.getProtocolsTaker(protocol);
        if (taker.isPresent()) {
            final WebSocketTaker wst = taker.get();
            final ResinWebSocketListener listener = new ResinWebSocketListener(wst);
            res.setHeader("Sec-WebSocket-Protocol", protocol);
            final WebSocketServletRequest wsRequest = (WebSocketServletRequest)
                    ((TransitionLogHttpServletRequestWrapper) request).getRequest();
            
            try {
                wsRequest.startWebSocket(listener);
            } catch (IllegalStateException e) {
                Logger.getLogger().info("websocket request error: {}", e.getMessage());
                return;
            }
            
            if (wst.processClass().isPresent()) {
                // start push event loop
                final Map<String, Object> param = new HashMap<>();
                param.put("key", listener.key);
                IntervalScheduler.getInstance().add(wst.processClass().get(), userCd, param);
            }
        } else {
            Logger.getLogger().info("invalid protocol: {}", protocol);
            res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }
    }

}