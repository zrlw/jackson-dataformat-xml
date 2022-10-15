 package com.fasterxml.jackson.dataformat.xml.ser;

import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.IndexedStringListSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.util.XmlUtil;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class NamespaceXmlBeanTokenBufferWriter extends BeanPropertyWriter {

    private static final long serialVersionUID = -8057737354057297079L;

    private QName _wrapperQName;

    private QName _xmlName;

    public NamespaceXmlBeanTokenBufferWriter(BeanPropertyWriter base, QName xmlName) {
        super(base, new PropertyName(xmlName.getLocalPart(), xmlName.getNamespaceURI()));
        JacksonXmlElementWrapper wrapperProp = base.getAnnotation(JacksonXmlElementWrapper.class);
        if (wrapperProp == null) {
            _wrapperQName = null;
        } else {
            String simpleName = wrapperProp.localName();
            if (simpleName.isEmpty()) {
                simpleName = xmlName.getLocalPart();
            }
            String prefix = "";
            int idx = simpleName.indexOf(':');
            if (idx >= 0) {
                prefix = simpleName.substring(0, idx);
                simpleName = simpleName.substring(idx + 1);
            }
            if (prefix.isEmpty() && !StringUtils.isEmpty(wrapperProp.namespace())) {
                prefix = XmlUtil.DEFAULT_NAMESPACE_PREFIX + wrapperProp.namespace().hashCode();
            }
            _wrapperQName = new QName(wrapperProp.namespace(), simpleName, prefix);
        }
        this._xmlName = xmlName;
    }

    public QName getXmlName() {
        return _xmlName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serializeAsField(Object bean, JsonGenerator gen,
            SerializerProvider prov) throws Exception {
        // inlined 'get()'
        final Object value = (_accessorMethod == null) ? _field.get(bean)
                : _accessorMethod.invoke(bean, (Object[]) null);

        // Null handling is bit different, check that first
        if (value == null) {
            if (_nullSerializer != null) {
                gen.writeFieldName(_name);
                _nullSerializer.serialize(null, gen, prov);
            }
            return;
        }
        // then find serializer to use
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap m = _dynamicSerializers;
            ser = m.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(m, cls, prov);
            }
        }
        // and then see if we must suppress certain values (default, empty)
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (ser.isEmpty(prov, value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }
        // For non-nulls: simple check for direct cycles
        if (value == bean) {
            // four choices: exception; handled by call; pass-through or write null
            if (_handleSelfReference(bean, gen, prov, ser)) {
                return;
            }
        }

        if (_wrapperQName != null) {
            gen.writeFieldName(_wrapperQName.getLocalPart());
            gen.writeStartObject();
        }

        gen.writeFieldName(_name);

        if (ser instanceof NamespaceXmlBeanSerializer) {
            if (_typeSerializer == null) {
                ((NamespaceXmlBeanSerializer) ser).serialize(value, gen, prov, _xmlName);
            } else {
                ((NamespaceXmlBeanSerializer) ser).serializeWithType(value, gen, prov, _typeSerializer, _xmlName);
            }
        } else if (ser instanceof StringSerializer) {
            if (!_xmlName.getPrefix().isEmpty() || !_xmlName.getNamespaceURI().isEmpty()) {
                gen.writeStartObject();
                gen.writeFieldName(XmlUtil.COMPLEX_NODE_TEXT_TAG);
            }

            if (_typeSerializer == null) {
                ser.serialize(value, gen, prov);
            } else {
                ser.serializeWithType(value, gen, prov, _typeSerializer);
            }

            if (!_xmlName.getPrefix().isEmpty() || !_xmlName.getNamespaceURI().isEmpty()) {
                String prefix = _xmlName.getPrefix();
                if (!prefix.isEmpty()) {
                    gen.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                    gen.writeString(prefix);
                } else if (!_xmlName.getNamespaceURI().isEmpty()) {
                    prefix = XmlUtil.DEFAULT_NAMESPACE_PREFIX + _xmlName.getNamespaceURI().hashCode();
                    gen.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                    gen.writeString(prefix);
                }
                
                if (!_xmlName.getNamespaceURI().isEmpty()) {
                    gen.writeFieldName(XmlUtil.NAMESPACES_TAG);
                    gen.writeStartObject();
                    gen.writeFieldName(prefix);
                    gen.writeString(_xmlName.getNamespaceURI());
                    gen.writeEndObject();
                }
                gen.writeEndObject();
            }
        } else if (ser.getClass() == IndexedStringListSerializer.class) {
            List<String> listValue = (List<String>) value;
            final int len = listValue.size();
            gen.writeStartArray(value, len);
            serializeArrayContents(listValue, gen, prov, len);
            gen.writeEndArray();
        } else {
            if (_typeSerializer == null) {
                ser.serialize(value, gen, prov);
            } else {
                ser.serializeWithType(value, gen, prov, _typeSerializer);
            }
        }

        if (_wrapperQName != null) {
            if (!_wrapperQName.getPrefix().isEmpty()) {
                gen.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                gen.writeString(_wrapperQName.getPrefix());

                if (!_wrapperQName.getNamespaceURI().isEmpty()) {
                    gen.writeFieldName(XmlUtil.NAMESPACES_TAG);
                    gen.writeStartObject();
                    gen.writeFieldName(_wrapperQName.getPrefix());
                    gen.writeString(_wrapperQName.getNamespaceURI());
                    gen.writeEndObject();
                }
            }
            gen.writeEndObject();
        }
    }

    private void serializeArrayContents(List<String> value, JsonGenerator g,
            SerializerProvider provider, int len) throws IOException
    {
        for (int i = 0; i < len; ++i) {
            String str = value.get(i);
            if (str == null) {
                provider.defaultSerializeNull(g);
            } else {
                if (!_xmlName.getPrefix().isEmpty() || !_xmlName.getNamespaceURI().isEmpty()) {
                    g.writeStartObject();
                    g.writeFieldName(XmlUtil.COMPLEX_NODE_TEXT_TAG);
                }
                
                g.writeString(str);
                
                if (!_xmlName.getPrefix().isEmpty() || !_xmlName.getNamespaceURI().isEmpty()) {
                    String prefix = _xmlName.getPrefix();
                    if (!prefix.isEmpty()) {
                        g.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                        g.writeString(prefix);
                    } else if (!_xmlName.getNamespaceURI().isEmpty()) {
                        prefix = XmlUtil.DEFAULT_NAMESPACE_PREFIX + _xmlName.getNamespaceURI().hashCode();
                        g.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                        g.writeString(prefix);
                    }

                    if (!_xmlName.getNamespaceURI().isEmpty()) {
                        g.writeFieldName(XmlUtil.NAMESPACES_TAG);
                        g.writeStartObject();
                        g.writeFieldName(prefix);
                        g.writeString(_xmlName.getNamespaceURI());
                        g.writeEndObject();
                    }
                    g.writeEndObject();
                }
            }
        }
    }
}
