 package com.fasterxml.jackson.dataformat.xml;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.ser.NamespaceXmlSerializerProvider;
import com.fasterxml.jackson.dataformat.xml.util.XmlRootNameLookup;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class NamespaceXmlMapper extends XmlMapper {

    private static final long serialVersionUID = -7996440124093069230L;

    public NamespaceXmlMapper() {
         super(new NamespaceXmlFactory(), new NamespaceXmlModule());

         _deserializationConfig = _deserializationConfig
             .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
             .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
             .with(new NamespaceXmlAnnotationIntrospector());

         setSerializationInclusion(Include.NON_NULL);

         _serializerProvider = new NamespaceXmlSerializerProvider(new XmlRootNameLookup());
     }
}
