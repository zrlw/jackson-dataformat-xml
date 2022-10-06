 package com.fasterxml.jackson.dataformat.xml.ser;

import java.io.IOException;
import java.util.BitSet;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.AnyGetterWriter;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.xml.util.XmlUtil;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class NamespaceXmlBeanSerializer extends XmlBeanSerializer {

    private static final long serialVersionUID = 1673233865438045971L;

    private AnyGetterWriter _namespaceAnyGetterWriter = null;

    private BeanPropertyWriter[] beanProps = null;

    private NamespaceXmlBeanTokenBufferWriter[] namespaceProps = null;

    public NamespaceXmlBeanSerializer(XmlBeanSerializer src) {
        super(src, null);

        for (int i = 0, len = _xmlNames.length; i < len; ++i) {
            QName qname = _xmlNames[i];
            int idx = qname.getLocalPart().indexOf(':');
            if (idx >= 0) {
                String prefix = qname.getLocalPart().substring(0, idx);
                String localPart = qname.getLocalPart().substring(idx + 1);
                _xmlNames[i] = new QName(qname.getNamespaceURI(), localPart, prefix);
            }
        }
    }

    @Override
    public void serialize(Object bean, JsonGenerator g, SerializerProvider provider) throws IOException
    {
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, g, provider, true);
            return;
        }
        if (g instanceof NamespaceXmlBeanToXmlGenerator) {
            ((NamespaceXmlBeanToXmlGenerator) g).writeStartNamespaceObject();
        } else {
            g.writeStartObject();
        }
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, g, provider);
        } else {
            serializeFields(bean, g, provider);
        }
        g.writeEndObject();
    }

    public void serialize(Object bean, JsonGenerator g, SerializerProvider provider, QName xmlName) throws IOException
    {
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, g, provider, true);
            return;
        }
        g.writeStartObject();
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, g, provider);
        } else {
            serializeFields(bean, g, provider);
        }

        if (g instanceof TokenBuffer && xmlName != null) {
            String prefix = xmlName.getPrefix();
            if (!prefix.isEmpty()) {
                g.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                g.writeString(prefix);
            } else if (!xmlName.getNamespaceURI().isEmpty()) {
                prefix = XmlUtil.DEFAULT_NAMESPACE_PREFIX + xmlName.getNamespaceURI().hashCode();
                g.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                g.writeString(prefix);
            }

            if (!xmlName.getNamespaceURI().isEmpty()) {
                g.writeFieldName(XmlUtil.NAMESPACES_TAG);
                g.writeStartObject();
                g.writeFieldName(prefix);
                g.writeString(xmlName.getNamespaceURI());
                g.writeEndObject();
            }
        }

        g.writeEndObject();
    }

    public void serializeWithType(Object bean, JsonGenerator gen, SerializerProvider provider,
        TypeSerializer typeSer, QName xmlName)
        throws IOException {
        if (_objectIdWriter != null) {
            // 08-Jul-2021, tatu: Should NOT yet set, would override "parent"
            // context (wrt [databind#3160]
            // gen.setCurrentValue(bean);
            _serializeWithObjectId(bean, gen, provider, typeSer);
            return;
        }

        WritableTypeId typeIdDef = _typeIdDef(typeSer, bean, JsonToken.START_OBJECT);
        typeSer.writeTypePrefix(gen, typeIdDef);
        gen.setCurrentValue(bean); // [databind#878]
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, gen, provider);
        } else {
            serializeFields(bean, gen, provider);
        }

        if (gen instanceof TokenBuffer && xmlName != null) {
            String prefix = xmlName.getPrefix();
            if (!prefix.isEmpty()) {
                gen.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                gen.writeString(prefix);
            } else if (!xmlName.getNamespaceURI().isEmpty()) {
                prefix = XmlUtil.DEFAULT_NAMESPACE_PREFIX + xmlName.getNamespaceURI().hashCode();
                gen.writeFieldName(XmlUtil.NAMESPACE_PREFIX_TAG);
                gen.writeString(prefix);
            }

            if (!xmlName.getNamespaceURI().isEmpty()) {
                gen.writeFieldName(XmlUtil.NAMESPACES_TAG);
                gen.writeStartObject();
                gen.writeFieldName(prefix);
                gen.writeString(xmlName.getNamespaceURI());
                gen.writeEndObject();
            }
        }

        typeSer.writeTypeSuffix(gen, typeIdDef);
    }

    @Override
    protected void serializeFields(Object bean, JsonGenerator gen0, SerializerProvider provider)
        throws IOException
    {
        if (gen0 instanceof TokenBuffer) {
            serializeFieldsOfTokenBuffer(bean, (TokenBuffer) gen0, provider);
            return;
        }

        // 19-Aug-2013, tatu: During 'convertValue()', need to skip
        if (!(gen0 instanceof NamespaceXmlBeanToXmlGenerator)) {
            super.serializeFields(bean, gen0, provider);
            return;
        }
        final NamespaceXmlBeanToXmlGenerator xgen = (NamespaceXmlBeanToXmlGenerator) gen0;
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }

        if (beanProps == null) {
            beanProps = new BeanPropertyWriter[props.length];
        }

        final int attrCount = _attributeCount;
        if (attrCount > 0) {
            xgen.setNextIsAttribute(true);
        }
        final int textIndex = _textPropertyIndex;
        final QName[] xmlNames = _xmlNames;
        int i = 0;
        final BitSet cdata = _cdata;

        try {
            final boolean isAttribute = XmlUtil.getFieldValue("_nextIsAttribute", ToXmlGenerator.class, xgen);
            
            for (final int len = props.length; i < len; ++i) {
                // 28-jan-2014, pascal: we don't want to reset the attribute flag if we are an unwrapping serializer
                // that started with nextIsAttribute to true because all properties should be unwrapped as attributes too.
                if (i == attrCount && !(isAttribute && isUnwrappingSerializer())) {
                    xgen.setNextIsAttribute(false);
                }
                // also: if this is property to write as text ("unwrap"), need to:
                if (i == textIndex) {
                    xgen.setNextIsUnwrapped(true);
                }
                xgen.setNextName(xmlNames[i]);
                if (props[i] != null) { // can have nulls in filtered list
                    if (beanProps[i] == null) {
                        if (!xmlNames[i].getPrefix().isEmpty() && xmlNames[i].getNamespaceURI().isEmpty()) {
                            String namespace = xgen.getNamespace(xmlNames[i].getPrefix());
                            if (namespace != null) {
                                beanProps[i] = new NamespaceXmlBeanPropertyWriter(props[i],
                                    new QName(namespace, xmlNames[i].getLocalPart(), xmlNames[i].getPrefix()));
                            } else {
                                beanProps[i] = new NamespaceXmlBeanPropertyWriter(props[i], xmlNames[i]);
                            }
                        } else {
                            beanProps[i] = new NamespaceXmlBeanPropertyWriter(props[i], xmlNames[i]);
                        }
                    }
                    BeanPropertyWriter prop = beanProps[i];

                    if ((cdata != null) && cdata.get(i)) {
                        xgen.setNextIsCData(true);
                        prop.serializeAsField(bean, xgen, provider);
                        xgen.setNextIsCData(false);
                    } else {
                        prop.serializeAsField(bean, xgen, provider);
                    }
                }
                // Reset to avoid next value being written as unwrapped,
                // for example when property is suppressed
                if (i == textIndex) {
                    xgen.setNextIsUnwrapped(false);
                }
            }
            if (_anyGetterWriter != null) {
                // For [#117]: not a clean fix, but with @JsonTypeInfo, we'll end up
                // with accidental attributes otherwise
                xgen.setNextIsAttribute(false);
                if (_namespaceAnyGetterWriter == null ) {
                    MapSerializer mapSerializer =
                        XmlUtil.getFieldValue("_mapSerializer", AnyGetterWriter.class, _anyGetterWriter);
                    if (mapSerializer == null) {
                        _namespaceAnyGetterWriter = _anyGetterWriter;
                    } else {
                        BeanProperty property =
                            XmlUtil.getFieldValue("_property", AnyGetterWriter.class, _anyGetterWriter);
                        AnnotatedMember accessor =
                            XmlUtil.getFieldValue("_accessor", AnyGetterWriter.class, _anyGetterWriter);
                        Object filterId = XmlUtil.getFieldValue("_filterId", MapSerializer.class, mapSerializer);
                        boolean sortKeys = XmlUtil.getFieldValue("_sortKeys", MapSerializer.class, mapSerializer);
                        NamespaceXmlMapSerializer namespaceMapSerialier =
                            new NamespaceXmlMapSerializer(mapSerializer, filterId, sortKeys);
                        _namespaceAnyGetterWriter = new AnyGetterWriter(property, accessor, namespaceMapSerialier);
                    }
                }
                _namespaceAnyGetterWriter.getAndSerialize(bean, xgen, provider);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) { // Bit tricky, can't do more calls as stack is full; so:
            JsonMappingException mapE = JsonMappingException.from(gen0,
                    "Infinite recursion (StackOverflowError)");
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(new JsonMappingException.Reference(bean, name));
            throw mapE;
        }
    }

    protected void serializeFieldsOfTokenBuffer(Object bean, TokenBuffer gen, SerializerProvider provider)
        throws IOException
    {
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }

        if (namespaceProps == null) {
            namespaceProps = new NamespaceXmlBeanTokenBufferWriter[props.length];
        }

        final int attrCount = _attributeCount;
        final int textIndex = _textPropertyIndex;
        final QName[] xmlNames = _xmlNames;

        if (attrCount > 0) {
            gen.writeFieldName(XmlUtil.ATTRIBUTES_TAG);
            gen.writeStartObject();
        }
        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                NamespaceXmlBeanTokenBufferWriter namespaceProp = null;
                if (prop != null) { // can have nulls in filtered list
                    if (namespaceProps[i] == null) {
                        QName xmlName = null;
                        if (i == textIndex) {
                            xmlName = new QName(xmlNames[i].getNamespaceURI(), XmlUtil.COMPLEX_NODE_TEXT_TAG);
                        } else {
                            xmlName = xmlNames[i];
                        }
                        namespaceProp = new NamespaceXmlBeanTokenBufferWriter(prop, xmlName);
                        namespaceProps[i] = namespaceProp;
                    } else {
                        namespaceProp = namespaceProps[i];
                    }
                    namespaceProp.serializeAsField(bean, gen, provider);
                    if (attrCount > 0 && i == attrCount - 1) {
                        gen.writeEndObject();
                    }
                }
            }
            if (_anyGetterWriter != null) {
                if (_namespaceAnyGetterWriter == null) {
                    MapSerializer mapSerializer =
                        XmlUtil.getFieldValue("_mapSerializer", AnyGetterWriter.class, _anyGetterWriter);
                    if (mapSerializer == null) {
                        _namespaceAnyGetterWriter = _anyGetterWriter;
                    } else {
                        BeanProperty property =
                            XmlUtil.getFieldValue("_property", AnyGetterWriter.class, _anyGetterWriter);
                        AnnotatedMember accessor =
                            XmlUtil.getFieldValue("_accessor", AnyGetterWriter.class, _anyGetterWriter);
                        Object filterId = XmlUtil.getFieldValue("_filterId", MapSerializer.class, mapSerializer);
                        boolean sortKeys = XmlUtil.getFieldValue("_sortKeys", MapSerializer.class, mapSerializer);
                        NamespaceXmlMapSerializer namespaceMapSerialier =
                            new NamespaceXmlMapSerializer(mapSerializer, filterId, sortKeys);
                        _namespaceAnyGetterWriter = new AnyGetterWriter(property, accessor, namespaceMapSerialier);
                    }
                }
                _namespaceAnyGetterWriter.getAndSerialize(bean, gen, provider);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            // 04-Sep-2009, tatu: Dealing with this is tricky, since we don't have many
            // stack frames to spare... just one or two; can't make many calls.

            // 10-Dec-2015, tatu: and due to above, avoid "from" method, call ctor directly:
            // JsonMappingException mapE = JsonMappingException.from(gen, "Infinite recursion (StackOverflowError)", e);
            DatabindException mapE = new JsonMappingException(gen, "Infinite recursion (StackOverflowError)", e);

            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(bean, name);
            throw mapE;
        }
    }

}
