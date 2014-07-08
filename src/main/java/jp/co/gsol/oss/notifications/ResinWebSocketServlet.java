package jp.co.gsol.oss.notifications;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.websocket.WebSocketListener;
import com.caucho.websocket.WebSocketServletRequest;
import com.google.common.base.Optional;

import jp.co.intra_mart.common.platform.log.Logger;
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

    final Logger logger = Logger.getLogger();
    @Override
    public void service(final ServletRequest request, final ServletResponse response)
            throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse res = (HttpServletResponse) response;

        final String protocol = req.getHeader("Sec-WebSocket-Protocol");
        Optional<WebSocketExecutor> executor = WebSocketManager.getProtocolsExecutor(protocol);
        if (executor.isPresent()) {
            final WebSocketListener listener = new ResinWebSocketListener(executor.get(),
                    Contexts.get(AccountContext.class));
            res.setHeader("Sec-WebSocket-Protocol", protocol);
            final WebSocketServletRequest wsRequest = (WebSocketServletRequest)
                    ((TransitionLogHttpServletRequestWrapper) request).getRequest();
            wsRequest.startWebSocket(listener);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }
    }

}