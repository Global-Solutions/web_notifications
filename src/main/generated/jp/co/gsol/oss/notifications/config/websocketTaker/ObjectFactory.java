
package jp.co.gsol.oss.notifications.config.websocketTaker;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the jp.co.gsol.oss.notifications.config.websocketTaker package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: jp.co.gsol.oss.notifications.config.websocketTaker
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GsolWebsocketTaker }
     * 
     */
    public GsolWebsocketTaker createGsolWebsocketTaker() {
        return new GsolWebsocketTaker();
    }

    /**
     * Create an instance of {@link Taker }
     * 
     */
    public Taker createTaker() {
        return new Taker();
    }

}
