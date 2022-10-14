 package com.fasterxml.jackson.dataformat.xml.ser;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

/**
 * @author zrlw@sina.com
 * @date 2022/10/06
 */
public class NamespaceXmlBeanPropertyWriter extends BeanPropertyWriter {

    private static final long serialVersionUID = -6323509186594763963L;

    private QName _wrapperQName;

    private QName _xmlName;

    public NamespaceXmlBeanPropertyWriter(BeanPropertyWriter base, QName xmlName) {
        super(base, new PropertyName(xmlName.getLocalPart(), xmlName.getNamespaceURI()));
        if (_wrapperName == null || StringUtils.isEmpty(_wrapperName.getSimpleName())) {
            _wrapperQName = null;
        } else {
            String simpleName = _wrapperName.getSimpleName();
            String prefix = "";
            int idx = simpleName.indexOf(':');
            if (idx >= 0) {
                prefix = simpleName.substring(0, idx);
                simpleName = simpleName.substring(idx + 1);
            }
            _wrapperQName = new QName(_wrapperName.getNamespace(), simpleName, prefix);
        }
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
            // 20-Jun-2022, tatu: Defer checking of null, see [databind#3481]
            if((_suppressableValue != null)
                    && prov.includeFilterSuppressNulls(_suppressableValue)) {
                return;
            }
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

        final NamespaceXmlBeanToXmlGenerator xmlGen = (gen instanceof NamespaceXmlBeanToXmlGenerator)
                ? (NamespaceXmlBeanToXmlGenerator) gen : null;
        if (xmlGen != null) {
            xmlGen.startWrappedValue(_wrapperQName, _xmlName);            
        }

        // writeFieldName will remove prefix of next name.
        gen.writeFieldName(_name);

        // reset next name to restore prefix.
        if (!_xmlName.getPrefix().isEmpty() && _xmlName.getNamespaceURI().isEmpty()) {
            String namespace = xmlGen.getNamespace(_xmlName.getPrefix());
            if (namespace != null) {
                _xmlName = new QName(namespace, _xmlName.getLocalPart(), _xmlName.getPrefix());
            }
        }
        ((NamespaceXmlBeanToXmlGenerator) gen).setNextName(_xmlName);

        if (_typeSerializer == null) {
            ser.serialize(value, gen, prov);
        } else {
            ser.serializeWithType(value, gen, prov, _typeSerializer);
        }

        if (xmlGen != null) {
            xmlGen.finishWrappedValue(_wrapperQName, _xmlName);            
        }
    }

}
