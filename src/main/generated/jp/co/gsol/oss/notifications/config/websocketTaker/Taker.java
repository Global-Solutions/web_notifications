
package jp.co.gsol.oss.notifications.config.websocketTaker;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for taker complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="taker">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="component-name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="dicon-path" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "taker", namespace = "http://global-solutions.co.jp/notifications/config/gsol-websocket-taker", propOrder = {

})
public class Taker {

    @XmlElement(name = "component-name", namespace = "http://global-solutions.co.jp/notifications/config/gsol-websocket-taker", required = true)
    protected String componentName;
    @XmlElement(name = "dicon-path", namespace = "http://global-solutions.co.jp/notifications/config/gsol-websocket-taker", required = true)
    protected String diconPath;

    /**
     * Gets the value of the componentName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Sets the value of the componentName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComponentName(String value) {
        this.componentName = value;
    }

    /**
     * Gets the value of the diconPath property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDiconPath() {
        return diconPath;
    }

    /**
     * Sets the value of the diconPath property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDiconPath(String value) {
        this.diconPath = value;
    }

}
