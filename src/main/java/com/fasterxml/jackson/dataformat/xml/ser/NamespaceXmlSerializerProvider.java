 package com.fasterxml.jackson.dataformat.xml.ser;

import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.util.StaxUtil;
import com.fasterxml.jackson.dataformat.xml.util.TypeUtil;
import com.fasterxml.jackson.dataformat.xml.util.XmlRootNameLookup;

/**
  * @author zrlw@sina.com
  * @date 2022/10/04
  */
 public class NamespaceXmlSerializerProvider extends XmlSerializerProvider {

    private static final long serialVersionUID = -4495675921334563103L;

    public NamespaceXmlSerializerProvider(XmlRootNameLookup rootNames) {
         super(rootNames);
    }

    public NamespaceXmlSerializerProvider(XmlSerializerProvider src, SerializationConfig config,
            SerializerFactory f) {
        super(src, config, f);
    }

    @Override
    public DefaultSerializerProvider createInstance(SerializationConfig config, SerializerFactory jsf) {
        return new NamespaceXmlSerializerProvider(this, config, jsf);
    }

    @Override
    public void serializeValue(JsonGenerator gen, Object value) throws IOException {
        _generator = gen;
        if (value == null) {
            _serializeXmlNull(gen);
            return;
        }
        final Class<?> cls = value.getClass();
        final boolean asArray;
        final ToXmlGenerator xgen = _asXmlGenerator(gen);
        if (xgen == null) { // called by convertValue()
            asArray = false;
        } else {
            // [dataformat-xml#441]: allow ObjectNode unwrapping
            if (_shouldUnwrapObjectNode(xgen, value)) {
                _serializeUnwrappedObjectNode(xgen, value, null);
                return;
            }
            QName rootName = _rootNameFromConfig();
            if (rootName == null) {
                rootName = _rootNameLookup.findRootName(cls, _config);
            }
            int idx = rootName.getLocalPart().indexOf(':');
            if (idx >= 0) {
                String prefix = rootName.getLocalPart().substring(0, idx);
                String localPart = rootName.getLocalPart().substring(idx + 1);
                rootName = new QName(rootName.getNamespaceURI(), localPart, prefix);
            }
            _initWithRootName(xgen, rootName);
            asArray = TypeUtil.isIndexedType(cls);
            if (asArray) {
                _startRootArray(xgen, rootName);
            }
        }

        // From super-class implementation
        final JsonSerializer<Object> ser = findTypedValueSerializer(cls, true, null);
        try {
            String namespace = "";
            String localName = "";
            String prefix = "";
            QName rootName = null;
            if (gen instanceof TokenBuffer && ser instanceof NamespaceXmlBeanSerializer) {
                gen.writeStartObject();
                JacksonXmlRootElement xmlRootElem = cls.getAnnotation(JacksonXmlRootElement.class);
                if (xmlRootElem == null) {
                    JsonRootName jsonRootName = cls.getAnnotation(JsonRootName.class);
                    if (jsonRootName == null) {
                        localName = cls.getSimpleName();
                    } else {
                        namespace = jsonRootName.namespace();
                        if (jsonRootName.value() == null || jsonRootName.value().isEmpty()) {
                            localName = cls.getSimpleName();
                        } else {
                            localName = jsonRootName.value();
                        }
                    }
                } else {
                    namespace = xmlRootElem.namespace();
                    if (xmlRootElem.localName() == null || xmlRootElem.localName().isEmpty()) {
                        localName = cls.getSimpleName();
                    } else {
                        localName = xmlRootElem.localName();
                    }
                }
                int idx = localName.indexOf(':');
                if (idx >= 0) {
                    prefix = localName.substring(0, idx);
                    localName = localName.substring(idx + 1);
                }
                gen.writeFieldName(localName);
                rootName = new QName(namespace, localName, prefix);
                ((NamespaceXmlBeanSerializer)ser).serialize(value, gen, this, rootName);
                gen.writeEndObject();
            } else {
                ser.serialize(value, gen, this);
            }
        } catch (Exception e) { // but wrap RuntimeExceptions, to get path information
            throw _wrapAsIOE(gen, e);
        }
        // end of super-class implementation

        if (asArray) {
            gen.writeEndObject();
        }
    }

    protected void _initWithRootName(ToXmlGenerator xgen, QName rootName) throws IOException {
        // 28-Nov-2012, tatu: We should only initialize the root name if no name has been
        // set, as per [dataformat-xml#42], to allow for custom serializers to work.
        if (!xgen.setNextNameIfMissing(rootName)) {
            // however, if we are root, we... insist
            if (xgen.inRoot()) {
                xgen.setNextName(rootName);
            }
        }
        xgen.initGenerator();
        String ns = rootName.getNamespaceURI();
        // [dataformat-xml#26] If we just try writing root element with namespace,
        // we will get an explicit prefix. But we'd rather use the default
        // namespace, so let's try to force that.
        if (ns != null && ns.length() > 0) {
            try {
                if (rootName.getPrefix().isEmpty()) {
                    xgen.getStaxWriter().setDefaultNamespace(ns);
                } else {
                    xgen.getStaxWriter().setPrefix(rootName.getPrefix(), ns);
                }
            } catch (XMLStreamException e) {
                StaxUtil.throwAsGenerationException(e, xgen);
            }
        }
    }

}
