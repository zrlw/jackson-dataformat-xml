 package com.fasterxml.jackson.dataformat.xml.testutil;

import java.io.Serializable;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
@JacksonXmlRootElement(localName = "soap:Envelope", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
public class MyEnvelope<T> implements Serializable {

    private static final long serialVersionUID = -1150733786349768403L;

    @JacksonXmlProperty(localName = "abc:Title", namespace = "http://abc")
    private String title;
    
    @JacksonXmlProperty(localName = "soap:Body")
    private MyPacketBody<T> body;
    
    public MyEnvelope() {
        
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MyPacketBody<T> getBody() {
        return body;
    }

    public void setBody(MyPacketBody<T> body) {
        this.body = body;
    }

    @Override
    public int hashCode() {
        int hashcode = 0;
        if (title != null) {
            hashcode = title.hashCode() * 31;
        }
        if (body != null) {
            hashcode += body.hashCode();
        }
        return hashcode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof MyEnvelope)) {
            return false;
        }
        
        MyEnvelope<?> my1 = ((MyEnvelope<?>) o);
        if (my1.title == null) {
            if (this.title != null) {
                return false;
            }
        } else if (!my1.title.equals(this.title)) {
            return false;
        }
        
        if (my1.body == null) {
            if (this.body != null) {
                return false;
            }
            return true;
        }
        return my1.body.equals(this.body);
    }
}
