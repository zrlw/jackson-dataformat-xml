 package com.fasterxml.jackson.dataformat.xml.ser;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.xml.util.XmlUtil;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class NamespaceXmlBeanPropertyWriter extends BeanPropertyWriter {

    private static final long serialVersionUID = -8057737354057297079L;

    private QName _xmlName;

    public NamespaceXmlBeanPropertyWriter(BeanPropertyWriter base, QName xmlName) {
        super(base, new PropertyName(xmlName.getLocalPart(), xmlName.getNamespaceURI()));
        this._xmlName = xmlName;
    }

    public QName getXmlName() {
        return _xmlName;
    }

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

        String wrapperNamespace = "";
        String wrapperLocalName = "";
        String wrapperPrefix = "";
        if (_wrapperName != null &&
            _wrapperName.getSimpleName() != null && !_wrapperName.getSimpleName().isEmpty()) {
            wrapperNamespace = _wrapperName.getNamespace();
            wrapperLocalName = _wrapperName.getSimpleName();
            int idx = wrapperLocalName.indexOf(':');
            if (idx >= 0) {
                wrapperPrefix = wrapperLocalName.substring(0, idx);
                wrapperLocalName = wrapperLocalName.substring(idx + 1);
            }

            if (!wrapperLocalName.isEmpty()) {
                gen.writeFieldName(wrapperLocalName);
                gen.writeStartObject();
            }
        }

        gen.writeFieldName(_name);

        if (gen instanceof TokenBuffer && ser instanceof NamespaceXmlBeanSerializer) {
            if (_typeSerializer == null) {
                ((NamespaceXmlBeanSerializer) ser).serialize(value, gen, prov, _xmlName);
            } else {
                ((NamespaceXmlBeanSerializer) ser).serializeWithType(value, gen, prov, _typeSerializer, _xmlName);
            }
        } else {
            if (gen instanceof TokenBuffer &&
                (!_xmlName.getPrefix().isEmpty() || !_xmlName.getNamespaceURI().isEmpty())) {
                gen.writeStartObject();
                gen.writeFieldName(XmlUtil.COMPLEX_NODE_TEXT_TAG);
            }

            if (_typeSerializer == null) {
                ser.serialize(value, gen, prov);
            } else {
                ser.serializeWithType(value, gen, prov, _typeSerializer);
            }

            if (gen instanceof TokenBuffer &&
                (!_xmlName.getPrefix().isEmpty() || !_xmlName.getNamespaceURI().isEmpty())) {
                if (!_xmlName.getPrefix().isEmpty()) {
                    gen.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                    gen.writeString(_xmlName.getPrefix());
                }
                
                if (!_xmlName.getNamespaceURI().isEmpty()) {
                    gen.writeFieldName(XmlUtil.NAMESPACES_TAG);
                    gen.writeStartObject();
                    gen.writeFieldName(_xmlName.getPrefix());
                    gen.writeString(_xmlName.getNamespaceURI());
                    gen.writeEndObject();
                }
                gen.writeEndObject();
            }
        }

        if (!wrapperLocalName.isEmpty()) {
            if (!wrapperPrefix.isEmpty()) {
                gen.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                gen.writeString(wrapperPrefix);
            }

            if (wrapperNamespace != null && !wrapperNamespace.isEmpty()) {
                gen.writeFieldName(XmlUtil.NAMESPACES_TAG);
                gen.writeStartObject();
                gen.writeFieldName(wrapperPrefix);
                gen.writeString(wrapperNamespace);
                gen.writeEndObject();
            }
            gen.writeEndObject();
        }
    }
}
