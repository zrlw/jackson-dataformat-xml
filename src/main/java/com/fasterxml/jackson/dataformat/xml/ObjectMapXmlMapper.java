 package com.fasterxml.jackson.dataformat.xml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.io.SAXReader;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.deser.ObjectMapBeanDeserializerFactory;
import com.fasterxml.jackson.dataformat.xml.deser.XmlDeserializationContext;
import com.fasterxml.jackson.dataformat.xml.util.XmlUtil;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class ObjectMapXmlMapper extends XmlMapper {

    private static final long serialVersionUID = 8611344913878587231L;

    // SAXReader: not thread safe.
    private static ThreadLocal<SAXReader> SAX_READER = new ThreadLocal<SAXReader>() {
        @Override
        protected SAXReader initialValue() {
            return SAXReader.createDefault();
        }
    };

    public ObjectMapXmlMapper() {
        super();

        _deserializationConfig = _deserializationConfig
            .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .with(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .with(new NamespaceXmlAnnotationIntrospector());

        setSerializationInclusion(Include.NON_NULL);

        _deserializationContext = new XmlDeserializationContext(ObjectMapBeanDeserializerFactory.instance);
    }

    public static Map<String, Object> xmlToObjectMap(String xml) throws Exception {
        return xmlToObjectMap(xml, null);
    }

    public static Map<String, Object> xmlToObjectMap(String xml, Set<String> forceListSet) throws Exception {
        try (StringReader stringReader = new StringReader(xml);) {
            Document doc = SAX_READER.get().read(stringReader);
            Element element = doc.getRootElement();
            String fullName = element.getName();
            Map<String, Object> objectMap = new HashMap<>();
            xmlElementToObjectMap(objectMap, objectMap, element, fullName, forceListSet, null);
            return objectMap;
        }
    }

    @SuppressWarnings("unchecked")
    private static void xmlElementToObjectMap(Map<String, Object> objectMapRoot, Map<String, Object> objectMap,
            Element element, String fullName,
            Set<String> forceListSet, List<Object> objectList) {
        Map<String, Object> MapComplex = null;
        List<Namespace> strippedNamespaceList = stripEmptyNamespaceList(element.declaredNamespaces());
        if (StringUtils.isNotBlank(element.getNamespacePrefix())
                || (strippedNamespaceList != null && strippedNamespaceList.size() > 0)
                || (element.attributes() != null && element.attributes().size() > 0)) {
            MapComplex = new HashMap<String, Object>();

            // namespace prefix
            String elementPrefix = element.getNamespacePrefix();
            if (!elementPrefix.isEmpty()) {
                MapComplex.put(XmlUtil.NAMESPACE_PREFIX_TAG, elementPrefix);
            }

            // declared namespaces
            if (strippedNamespaceList != null && strippedNamespaceList.size() > 0) {
                Map<String, Object> namespaceMap = new HashMap<String, Object>();
                MapComplex.put(XmlUtil.NAMESPACES_TAG, namespaceMap);
                for (Namespace namespace : strippedNamespaceList) {
                    String prefix = namespace.getPrefix();
                    if (prefix.isEmpty()) {
                        prefix = XmlUtil.DEFAULT_NAMESPACE_PREFIX + namespace.getURI().hashCode();
                        if (elementPrefix.isEmpty()) {
                            elementPrefix = prefix;
                            MapComplex.put(XmlUtil.NAMESPACE_PREFIX_TAG, elementPrefix);
                        }
                    }
                    namespaceMap.put(prefix, namespace.getURI());
                }
            }

            // attributes
            if (element.attributes() != null && element.attributes().size() > 0) {
                Map<String, Object> attrMap = new HashMap<String, Object>();;
                MapComplex.put(XmlUtil.ATTRIBUTES_TAG, attrMap);
                for (Attribute attr : element.attributes()) {
                    if (StringUtils.isNotBlank(attr.getNamespacePrefix())) {
                        Map<String, Object> attrObject = new HashMap<String, Object>();
                        attrObject.put(XmlUtil.COMPLEX_NODE_TEXT_TAG, attr.getText());
                        attrObject.put(XmlUtil.NAMESPACE_PREFIX_TAG, attr.getNamespacePrefix());
                        attrMap.put(attr.getName(), attrObject);
                    } else {
                        attrMap.put(attr.getName(), attr.getText());
                    }
                }
            }
        }

        List<Element> elements = element.elements();
        if (elements == null || elements.size() == 0) {
            if (MapComplex != null) {
                MapComplex.put(XmlUtil.COMPLEX_NODE_TEXT_TAG, element.getText());
            }
            if (objectList == null) {
                if (MapComplex != null) {
                    objectMap.put(element.getName(), MapComplex);
                } else {
                    objectMap.put(element.getName(), element.getText());
                }
            } else {
                if (MapComplex != null) {
                    objectList.add(MapComplex);
                } else {
                    objectList.add(element.getText());
                }
            }
            return;
        }

        Set<String> listSet = new HashSet<>();
        Set<String> existedSet = new HashSet<>();
        for (Element child : elements) {
            String childFullName = fullName + "." + child.getName();
            
            if (forceListSet != null && forceListSet.contains(childFullName)) {
                listSet.add(child.getName());
                continue;
            }

            if (existedSet.contains(child.getName())) {
                listSet.add(child.getName());
                continue;
            }

            existedSet.add(child.getName());
        }

        Map<String, Object> mapChildren = null;
        if (MapComplex != null) {
            mapChildren = MapComplex;
        } else {
            mapChildren = new HashMap<String, Object>();
        }
        for (Element child : elements) {
            String childFullName = fullName + "." + child.getName();

            if (listSet.contains(child.getName())) {
                List<Object> childList = (List<Object>) mapChildren.get(child.getName());
                if (childList == null) {
                    childList = new ArrayList<>();
                    mapChildren.put(child.getName(), childList);
                }
                xmlElementToObjectMap(objectMapRoot, mapChildren, child, childFullName, forceListSet, childList);
            } else {
                xmlElementToObjectMap(objectMapRoot, mapChildren, child, childFullName, forceListSet, null);
            }
        }
        if (objectList == null) {
            objectMap.put(element.getName(), mapChildren);
        } else {
            objectList.add(mapChildren);
        }
    }
    
    private static List<Namespace> stripEmptyNamespaceList(List<Namespace> namespaceList) {
        if (namespaceList == null || namespaceList.size() == 0) {
            return null;
        }
        List<Namespace> strippedList = new ArrayList<>();
        for (Namespace namespace : namespaceList) {
            if (namespace.getPrefix().isEmpty() && namespace.getURI().isEmpty()) {
                continue;
            }
            strippedList.add(namespace);
        }
        return strippedList;
    }

    public static String objectMapToXml(Map<String, Object> objectMap) throws Exception {
        StringBuilder sb = new StringBuilder();
        objectMapToXmlBody(sb, objectMap);
        return XmlUtil.XML_HEADER + sb.toString();
    }

    private static void objectMapToXmlBody(StringBuilder sb, Map<String, Object> objectMap) {
        for (Entry<String, Object> nodeEntry : objectMap.entrySet()) {
            if (nodeEntry.getKey().equals(XmlUtil.NAMESPACE_PREFIX_TAG)
                    || nodeEntry.getKey().equals(XmlUtil.NAMESPACES_TAG)
                    || nodeEntry.getKey().equals(XmlUtil.ATTRIBUTES_TAG)) {
                    continue;
                }
            if (nodeEntry.getValue() instanceof List) {
                for (Object itemObject : (List<?>) nodeEntry.getValue()) {
                    buildXmlNode(sb, nodeEntry.getKey(), itemObject);
                }
            } else {
                buildXmlNode(sb, nodeEntry.getKey(), nodeEntry.getValue());
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void buildXmlNode(StringBuilder sb, String tagName, Object tagObject) {
        if (tagObject instanceof Map) {
            boolean isComplex = false;
            sb.append('<');
            if (((Map) tagObject).containsKey(XmlUtil.NAMESPACE_PREFIX_TAG)) {
                isComplex = true;
                sb.append(((Map) tagObject).get(XmlUtil.NAMESPACE_PREFIX_TAG));
                sb.append(':');
            }
            sb.append(tagName);

            if (((Map) tagObject).containsKey(XmlUtil.ATTRIBUTES_TAG)) {
                isComplex = true;
                for (Entry<String, Object> attrEntry
                        : ((Map<String, Object>) ((Map) tagObject).get(XmlUtil.ATTRIBUTES_TAG)).entrySet()) {
                    sb.append(' ');
                    if (attrEntry.getValue() instanceof Map) {
                        sb.append(((Map) attrEntry.getValue()).get(XmlUtil.NAMESPACE_PREFIX_TAG));
                        sb.append(':');
                        sb.append(attrEntry.getKey());
                        sb.append('=');
                        sb.append('"');
                        sb.append(escape((String) ((Map) attrEntry.getValue()).get(XmlUtil.COMPLEX_NODE_TEXT_TAG)));
                    } else {
                        sb.append(attrEntry.getKey());
                        sb.append('=');
                        sb.append('"');
                        sb.append(escape((String) attrEntry.getValue()));
                    }
                    sb.append('"');
                }
            }

            if (((Map) tagObject).containsKey(XmlUtil.NAMESPACES_TAG)) {
                isComplex = true;
                for (Entry<String, Object> namespaceEntry
                        : ((Map<String, Object>) ((Map) tagObject).get(XmlUtil.NAMESPACES_TAG)).entrySet()) {
                    sb.append(' ');
                    sb.append("xmlns");
                    if (!namespaceEntry.getKey().isEmpty()) {
                        sb.append(':');
                        sb.append(namespaceEntry.getKey());
                    }
                    sb.append('=');
                    sb.append('"');
                    sb.append(escape((String) namespaceEntry.getValue()));
                    sb.append('"');
                }
            }

            if (isComplex && ((Map) tagObject).containsKey(XmlUtil.COMPLEX_NODE_TEXT_TAG)) {
                String value = (String) ((Map) tagObject).get(XmlUtil.COMPLEX_NODE_TEXT_TAG);
                if (value == null || value.isEmpty()) {
                    sb.append("/>");
                    return;
                }
                sb.append('>');
                sb.append(escape((String) ((Map) tagObject).get(XmlUtil.COMPLEX_NODE_TEXT_TAG)));
            } else {
                sb.append('>');
                objectMapToXmlBody(sb, (Map) tagObject);
            }

            sb.append("</");
            if (((Map) tagObject).containsKey(XmlUtil.NAMESPACE_PREFIX_TAG)) {
                sb.append(((Map) tagObject).get(XmlUtil.NAMESPACE_PREFIX_TAG));
                sb.append(':');
            }
            sb.append(tagName);
            sb.append('>');
        } else if (((String) tagObject) == null || ((String) tagObject).isEmpty()) {
            sb.append('<');
            sb.append(tagName);
            sb.append("/>");
        } else {
            sb.append('<');
            sb.append(tagName);
            sb.append('>');
            sb.append(escape((String) tagObject));
            sb.append("</");
            sb.append(tagName);
            sb.append('>');
        }
    }
    
    private static String escape(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        for (final int cp : codePointIterator(string)) {
            switch (cp) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    if (mustEscape(cp)) {
                        sb.append("&#x");
                        sb.append(Integer.toHexString(cp));
                        sb.append(';');
                    } else {
                        sb.appendCodePoint(cp);
                    }
            }
        }
        return sb.toString();
    }

    private static Iterable<Integer> codePointIterator(final String string) {
        return new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    private int nextIndex = 0;
                    private int length = string.length();

                    @Override
                    public boolean hasNext() {
                        return this.nextIndex < this.length;
                    }

                    @Override
                    public Integer next() {
                        int result = string.codePointAt(this.nextIndex);
                        this.nextIndex += Character.charCount(result);
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    private static boolean mustEscape(int cp) {
        /* Valid range from https://www.w3.org/TR/REC-xml/#charsets
         *
         * #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
         *
         * any Unicode character, excluding the surrogate blocks, FFFE, and FFFF.
         */
        // isISOControl is true when (cp >= 0 && cp <= 0x1F) || (cp >= 0x7F && cp <= 0x9F)
        // all ISO control characters are out of range except tabs and new lines
        return (Character.isISOControl(cp) && cp != 0x9 && cp != 0xA && cp != 0xD) || !(
        // valid the range of acceptable characters that aren't control
        (cp >= 0x20 && cp <= 0xD7FF) || (cp >= 0xE000 && cp <= 0xFFFD) || (cp >= 0x10000 && cp <= 0x10FFFF));
    }

}
