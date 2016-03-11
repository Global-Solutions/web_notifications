
package jp.co.gsol.oss.notifications.config.websocketTaker;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="protocol" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="taker" type="{http://global-solutions.co.jp/notifications/config/gsol-websocket-taker}taker"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {

})
@XmlRootElement(name = "gsol-websocket-taker", namespace = "http://global-solutions.co.jp/notifications/config/gsol-websocket-taker")
public class GsolWebsocketTaker {

    @XmlElement(namespace = "http://global-solutions.co.jp/notifications/config/gsol-websocket-taker", required = true)
    protected String protocol;
    @XmlElement(namespace = "http://global-solutions.co.jp/notifications/config/gsol-websocket-taker", required = true)
    protected Taker taker;

    /**
     * Gets the value of the protocol property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the value of the protocol property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProtocol(String value) {
        this.protocol = value;
    }

    /**
     * Gets the value of the taker property.
     * 
     * @return
     *     possible object is
     *     {@link Taker }
     *     
     */
    public Taker getTaker() {
        return taker;
    }

    /**
     * Sets the value of the taker property.
     * 
     * @param value
     *     allowed object is
     *     {@link Taker }
     *     
     */
    public void setTaker(Taker value) {
        this.taker = value;
    }

}
