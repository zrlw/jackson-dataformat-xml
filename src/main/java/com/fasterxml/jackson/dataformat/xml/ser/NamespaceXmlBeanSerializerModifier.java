 package com.fasterxml.jackson.dataformat.xml.ser;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class NamespaceXmlBeanSerializerModifier extends XmlBeanSerializerModifier {

    private static final long serialVersionUID = -2482923928396201157L;

    @Override
     public JsonSerializer<?> modifySerializer(SerializationConfig config,
             BeanDescription beanDesc, JsonSerializer<?> serializer)
     {
         if (serializer instanceof XmlBeanSerializer) {
             return new NamespaceXmlBeanSerializer((XmlBeanSerializer) serializer);
         }
         return serializer;
     }

}
