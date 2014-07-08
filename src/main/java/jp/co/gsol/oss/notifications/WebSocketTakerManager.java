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

public class WebSocketTakerManager {

    static List<String> registeredProtocol(Collection<GsolWebsocketTaker> takers) {
        List<String> protos = new ArrayList<>();
        for (GsolWebsocketTaker taker : takers)
           protos.add(taker.getProtocol());
        return protos;
    }
    static public List<String> registerdProtocols() {
        try {
            return registeredProtocol(ConfigurationLoader.loadAll(GsolWebsocketTaker.class));
        } catch (ConfigurationException e) {
            // TODO
            Logger.getLogger().error("", e);
        }
        return new ArrayList<>();
    }
    static public Optional<WebSocketTaker> getProtocolsTaker(final String protocol) {
        try {
            for (GsolWebsocketTaker conf : ConfigurationLoader.loadAll(GsolWebsocketTaker.class)) {
                if (conf.getProtocol().equals(protocol)) {
                    final S2Container cont = S2ContainerFactory.create(conf.getTaker().getDiconPath());
                    final Object taker = cont.getComponent(conf.getTaker().getComponentName());
                    if (taker instanceof WebSocketTaker)
                        return Optional.<WebSocketTaker>of((WebSocketTaker) taker);
                }
            }
        } catch (ComponentNotFoundRuntimeException
                | TooManyRegistrationRuntimeException
                | CyclicReferenceRuntimeException
                | ConfigurationException
                | ClassNotFoundRuntimeException e) {
            // TODO
            Logger.getLogger().error("some configuration is wrong", e);
        }

        return Optional.absent();
    }
}
