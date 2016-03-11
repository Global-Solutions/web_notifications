package jp.co.gsol.oss.notifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.seasar.framework.container.ComponentNotFoundRuntimeException;
import org.seasar.framework.container.CyclicReferenceRuntimeException;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.TooManyRegistrationRuntimeException;
import org.seasar.framework.container.factory.S2ContainerFactory;
import org.seasar.framework.exception.ClassNotFoundRuntimeException;

import jp.co.gsol.oss.notifications.config.websocketTaker.GsolWebsocketTaker;
import jp.co.intra_mart.common.platform.log.Logger;
import jp.co.intra_mart.foundation.config.ConfigurationException;
import jp.co.intra_mart.foundation.config.ConfigurationLoader;

import com.google.common.base.Optional;

/**
 * Manager for WebSocket event handlers.
 * @author Global Solutions company limited
 */
public final class WebSocketTakerManager {

    /** .*/
    private WebSocketTakerManager() { }

    /**
     * map for #{@link GsolWebsocketTaker} collection -> it's protocol name list.
     * @param takers WebSocket event handler's configuration collection
     * @return protocol name list
     */
    private static List<String> registeredProtocol(final Collection<GsolWebsocketTaker> takers) {
        final List<String> protos = new ArrayList<>();
        for (GsolWebsocketTaker taker : takers)
           protos.add(taker.getProtocol());
        return protos;
    }
    /**
     * registered protocol's list on the web app.
     * @return protocol list
     */
    public static List<String> registerdProtocols() {
        try {
            return registeredProtocol(ConfigurationLoader.loadAll(GsolWebsocketTaker.class));
        } catch (final ConfigurationException e) {
            Logger.getLogger().error("some configurations are wrong", e);
        }
        return new ArrayList<>();
    }
    /**
     * fetch protocol's WebSocket event handler.
     * @param protocol protocol name
     * @return WebSocket event handler
     */
    public static Optional<WebSocketTaker> getProtocolsTaker(final String protocol) {
        try {
            for (GsolWebsocketTaker conf : ConfigurationLoader.loadAll(GsolWebsocketTaker.class)) {
                if (conf.getProtocol().equals(protocol)) {
                    final S2Container cont = S2ContainerFactory.create(conf.getTaker().getDiconPath());
                    final Object taker = cont.getComponent(conf.getTaker().getComponentName());
                    if (taker instanceof WebSocketTaker)
                        return Optional.of((WebSocketTaker) taker);
                }
            }
        } catch (
            final ComponentNotFoundRuntimeException
                | TooManyRegistrationRuntimeException
                | CyclicReferenceRuntimeException
                | ConfigurationException
                | ClassNotFoundRuntimeException e) {
            Logger.getLogger().error("some configurations are wrong", e);
        }

        return Optional.absent();
    }
}
