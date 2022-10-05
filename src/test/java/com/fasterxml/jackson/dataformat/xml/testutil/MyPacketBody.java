 package com.fasterxml.jackson.dataformat.xml.testutil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
@JacksonXmlRootElement(localName = "soap:Body")
public class MyPacketBody<T> implements Serializable {

    private static final long serialVersionUID = -5675240498770647425L;

    @JsonIgnore
    private T t;

    public MyPacketBody() {
        
    }

    public MyPacketBody(T t){
      this.setT(t);
    }

    public T getT() {
        return t;
    }

    public void setT(T t) {
        this.t = t;
    }

    @JsonAnyGetter
    public Map<String, Object> getAny() {
        Map<String, Object> map = new HashMap<>();
        map.put(t.getClass().getAnnotation(JacksonXmlRootElement.class).localName(), t);
        return map;
    }
    
    @JsonAnySetter
    public void setAny(String name, T t) {
        // the parameter 'name' does not have namespace prefix.
        String annotationName = t.getClass().getAnnotation(JacksonXmlRootElement.class).localName();
        int idx = annotationName.indexOf(':');
        if (idx >= 0) {
            annotationName = annotationName.substring(idx + 1);
        }
        if (!name.equals(annotationName)) {
            throw new RuntimeException("unkown field name: " + name);
        }
        this.t = t;
    }

    @Override
    public int hashCode() {
        return t == null ? 0 : t.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof MyPacketBody)) {
            return false;
        }
        
        Object myT = ((MyPacketBody<?>)o).t;
        if (!myT.getClass().isInstance(t)) {
            return false;
        }
        return myT.equals(t);
    }
}
