package com.fasterxml.jackson.dataformat.xml.util;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.NamespaceXmlMapper;
import com.fasterxml.jackson.dataformat.xml.ObjectMapXmlMapper;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class XmlUtil {

    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    public static final String NAMESPACES_TAG = "__namespaces";

    public static final String NAMESPACE_PREFIX_TAG = "__prefix";

    public static final String ATTRIBUTES_TAG = "__attributes";

    public static final String COMPLEX_NODE_TEXT_TAG = "__text";

    public static final String DEFAULT_NAMESPACE_PREFIX = "ns";
    
    private static final NamespaceXmlMapper NAMESPACE_XML_MAPPER = new NamespaceXmlMapper();

    private static final ObjectMapXmlMapper OBJECTMAP_XML_MAPPER = new ObjectMapXmlMapper();

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<Map<String, Object>>() {};

    public static NamespaceXmlMapper getNamespaceXmlMapper() {
        return NAMESPACE_XML_MAPPER;
    }

    public static ObjectMapXmlMapper getObjectMapXmlMapper() {
        return OBJECTMAP_XML_MAPPER;
    }

    public static <T> T xmlToObject(String xmlMessage, Class<T> clazz) throws Exception {
        return NAMESPACE_XML_MAPPER.readValue(xmlMessage, clazz);
    }

    public static <T> T xmlToObject(String xmlMessage, TypeReference<T> typeReference) throws Exception {
        return NAMESPACE_XML_MAPPER.readValue(xmlMessage, typeReference);
    }

    public static Map<String, Object> xmlToObjectMap(String xml) throws Exception {
        return xmlToObjectMap(xml, null);
    }

    public static Map<String, Object> xmlToObjectMap(String xml, Set<String> forceListSet) throws Exception {
        return ObjectMapXmlMapper.xmlToObjectMap(xml, forceListSet);
    }

    public static <T> String objectToXml(T object) throws Exception {
        return XML_HEADER + NAMESPACE_XML_MAPPER.writeValueAsString(object);
    }

    public static <T> Map<String, Object> objectToObjectMap(T object) throws Exception {
        return NAMESPACE_XML_MAPPER.convertValue(object, MAP_TYPE_REFERENCE);
    }

    public static <T> byte[] objectToBytes(T object) throws Exception {
        return NAMESPACE_XML_MAPPER.writeValueAsBytes(object);
    }

    public static <T> T bytesToObject(byte[] bytes, Class<T> clazz) throws Exception {
        return NAMESPACE_XML_MAPPER.readValue(bytes, clazz);
    }
    
    public static <T> T bytesToObject(byte[] bytes, TypeReference<T> typeReference) throws Exception {
        return NAMESPACE_XML_MAPPER.readValue(bytes, typeReference);
    }

    public static String objectMapToXml(Map<String, Object> objectMap) throws Exception {
        return ObjectMapXmlMapper.objectMapToXml(objectMap);
    }

    public static <T> T objectMapToObject(Map<String, Object> objectMap, Class<T> clazz) throws Exception {
        return OBJECTMAP_XML_MAPPER.readValue(objectToBytes(objectMap), clazz);
    }

    public static <T> T objectMapToObject(Map<String, Object> objectMap, TypeReference<T> typeReference) throws Exception {
        return OBJECTMAP_XML_MAPPER.readValue(objectToBytes(objectMap), typeReference);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(String fieldName, Class<?> clazz, Object object)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(object);
    }
}
