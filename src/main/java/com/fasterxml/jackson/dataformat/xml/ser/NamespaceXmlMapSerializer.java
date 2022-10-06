 package com.fasterxml.jackson.dataformat.xml.ser;

import java.io.IOException;
import java.util.Map;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class NamespaceXmlMapSerializer extends MapSerializer {

    private static final long serialVersionUID = -7411558011648534365L;

    public NamespaceXmlMapSerializer(MapSerializer mapSerializer, Object filterId, boolean sortKeys) {
        super(mapSerializer, filterId, sortKeys);
    }

    public void serializeOptionalFields(Map<?, ?> value, JsonGenerator gen, SerializerProvider provider,
        Object suppressableValue) throws IOException {
        // If value type needs polymorphic type handling, some more work needed:
        if (_valueTypeSerializer != null) {
            serializeTypedFields(value, gen, provider, suppressableValue);
            return;
        }
        final boolean checkEmpty = (MARKER_FOR_EMPTY == suppressableValue);

        for (Map.Entry<?, ?> entry : value.entrySet()) {
            // First find key serializer
            Object keyElem = entry.getKey();
            JsonSerializer<Object> keySerializer;
            if (keyElem == null) {
                keySerializer = provider.findNullKeySerializer(_keyType, _property);
            } else {
                if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(keyElem)) {
                    continue;
                }
                keySerializer = _keySerializer;
            }

            // Then value serializer
            final Object valueElem = entry.getValue();
            JsonSerializer<Object> valueSer;
            if (valueElem == null) {
                if (_suppressNulls) { // all suppressions include null-suppression
                    continue;
                }
                valueSer = provider.getDefaultNullValueSerializer();
            } else {
                valueSer = _valueSerializer;
                if (valueSer == null) {
                    valueSer = _findSerializer(provider, valueElem);
                }
                // also may need to skip non-empty values:
                if (checkEmpty) {
                    if (valueSer.isEmpty(provider, valueElem)) {
                        continue;
                    }
                } else if (suppressableValue != null) {
                    if (suppressableValue.equals(valueElem)) {
                        continue;
                    }
                }
            }

            String namespace = "";
            String prefix = "";
            QName nextName = null;
            if (keyElem instanceof String) {
                int idx = ((String)keyElem).indexOf(':');
                if (idx >= 0) {
                    prefix = ((String)keyElem).substring(0, idx);
                    keyElem = ((String)keyElem).substring(idx + 1);
                }

                Class<?> cls = valueElem.getClass();
                JacksonXmlRootElement xmlRootElem = cls.getAnnotation(JacksonXmlRootElement.class);
                if (xmlRootElem == null) {
                    JsonRootName jsonRootName = cls.getAnnotation(JsonRootName.class);
                    if (jsonRootName != null) {
                        namespace = jsonRootName.namespace();
                    }
                } else {
                    namespace = xmlRootElem.namespace();
                }

                if (gen instanceof NamespaceXmlBeanToXmlGenerator) {
                    nextName = new QName(namespace, (String)keyElem, prefix);
                    ((NamespaceXmlBeanToXmlGenerator) gen).setNextName(nextName);
                } else if (gen instanceof TokenBuffer && valueSer instanceof NamespaceXmlBeanSerializer) {
                    nextName = new QName(namespace, (String)keyElem, prefix);
                }

            }
            // and then serialize, if all went well
            try {
                keySerializer.serialize(keyElem, gen, provider);
                if (gen instanceof NamespaceXmlBeanToXmlGenerator) {
                    // reset next name to restore prefix because it was removed at keySerializer#serialize.
                    ((NamespaceXmlBeanToXmlGenerator) gen).setNextName(nextName);
                    valueSer.serialize(valueElem, gen, provider);
                } else if (gen instanceof TokenBuffer && valueSer instanceof NamespaceXmlBeanSerializer) {
                    ((NamespaceXmlBeanSerializer)valueSer).serialize(valueElem, gen, provider, nextName);
                } else {
                    valueSer.serialize(valueElem, gen, provider);
                }
            } catch (Exception e) {
                wrapAndThrow(provider, e, value, String.valueOf(keyElem));
            }
        }
    }

    private final JsonSerializer<Object> _findSerializer(SerializerProvider provider, Object value)
        throws JsonMappingException {
        final Class<?> cc = value.getClass();
        JsonSerializer<Object> valueSer = _dynamicValueSerializers.serializerFor(cc);
        if (valueSer != null) {
            return valueSer;
        }
        if (_valueType.hasGenericTypes()) {
            return _findAndAddDynamic(_dynamicValueSerializers, provider.constructSpecializedType(_valueType, cc),
                provider);
        }
        return _findAndAddDynamic(_dynamicValueSerializers, cc, provider);
    }

}
