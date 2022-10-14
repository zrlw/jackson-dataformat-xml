 package com.fasterxml.jackson.dataformat.xml.deser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.BeanPropertyMap;
import com.fasterxml.jackson.databind.deser.impl.MethodProperty;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringCollectionDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.util.XmlUtil;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class ObjectMapBeanDeserializer extends BeanDeserializer {

    private static final long serialVersionUID = 2918795439591205404L;

    public ObjectMapBeanDeserializer(BeanDeserializerBuilder builder, BeanDescription beanDesc,
            BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs,
            HashSet<String> ignorableProps, boolean ignoreAllUnknown, Set<String> includableProps,
            boolean hasViews) {
        super(builder, beanDesc, properties, backRefs, ignorableProps, ignoreAllUnknown,
            includableProps, hasViews);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // common case first
        if (p.isExpectedStartObjectToken()) {
            if (_vanillaProcessing) {
                return vanillaDeserialize(p, ctxt, p.nextToken());
            }
            // 23-Sep-2015, tatu: This is wrong at some many levels, but for now... it is
            //    what it is, including "expected behavior".
            p.nextToken();
            if (_objectIdReader != null) {
                return deserializeWithObjectId(p, ctxt);
            }
            return deserializeFromObject(p, ctxt);
        }
        return _deserializeOther(p, ctxt, p.currentToken());
    }

    private final Object vanillaDeserialize(JsonParser p,
            DeserializationContext ctxt, JsonToken t)
        throws IOException
    {
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);
        if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
            String propName = p.currentName();
            do {
                p.nextToken();
                if (propName.equals(XmlUtil.NAMESPACES_TAG)) {
                    // skip START_OBJECT of NAMESPACES_TAG
                    p.nextToken();
                    // get namespace name
                    propName = p.currentName();
                    do {
                        // skip value of namespace
                        p.nextToken();
                    } while ((propName = p.nextFieldName()) != null);
                    propName = p.nextFieldName();
                    continue;
                } else if (propName.equals(XmlUtil.NAMESPACE_PREFIX_TAG)) {
                    // skip value of NAMESPACE_PREFIX_TAG
                    propName = p.nextFieldName();
                    continue;
                } else if (propName.equals(XmlUtil.ATTRIBUTES_TAG)) {
                    // skip START_OBJECT of ATTRIBUTES_TAG
                    p.nextToken();
                    // get attribute name
                    propName = p.currentName();
                    do {
                        p.nextToken();
                        if (propName.equals(XmlUtil.NAMESPACES_TAG)) {
                            // skip START_OBJECT of NAMESPACES_TAG
                            p.nextToken();
                            // get namespace name
                            propName = p.currentName();
                            do {
                                // skip value of namespace
                                p.nextToken();
                            } while ((propName = p.nextFieldName()) != null);
                            continue;
                        } else if (propName.equals(XmlUtil.NAMESPACE_PREFIX_TAG)) {
                            // skip value of NAMESPACE_PREFIX_TAG
                            continue;
                        }
                        SettableBeanProperty prop = _beanProperties.find(propName);

                        if (prop != null) { // normal case
                            try {
                                prop.deserializeAndSet(p, ctxt, bean);
                            } catch (Exception e) {
                                wrapAndThrow(e, bean, propName, ctxt);
                            }
                            continue;
                        }
                        handleUnknownVanilla(p, ctxt, bean, propName);
                    } while ((propName = p.nextFieldName()) != null);
                    propName = p.nextFieldName();
                    continue;
                }
                
                SettableBeanProperty prop = _beanProperties.find(propName);

                if (prop != null) { // normal case
                    boolean isWrapped = false;
                    boolean isComplex = false;
                    JsonDeserializer<Object> deser = prop.getValueDeserializer();
                    if (prop instanceof MethodProperty
                        && !(deser instanceof ObjectMapBeanDeserializer)
                        && !(deser instanceof DelegatingDeserializer
                            && ((DelegatingDeserializer) deser).getDelegatee() instanceof ObjectMapBeanDeserializer)) {
                        MethodProperty methodProp = (MethodProperty) prop;
                        JacksonXmlElementWrapper wrapperProp = methodProp.getAnnotation(JacksonXmlElementWrapper.class);
                        if (wrapperProp != null) {
                            if (!StringUtils.isEmpty(wrapperProp.localName())) {
                                isWrapped = true;
                                skipNamespaceTags(p, true);
                            }
                        }
                        
                        JacksonXmlProperty xmlProp = methodProp.getAnnotation(JacksonXmlProperty.class);
                        if (xmlProp == null) {
                            JsonProperty jsonProp = methodProp.getAnnotation(JsonProperty.class);
                            if (jsonProp != null) {
                                if (!StringUtils.isEmpty(jsonProp.value()) && jsonProp.value().indexOf(':') >= 0) {
                                    isComplex = true;
                                } else if (!StringUtils.isEmpty(jsonProp.namespace())) {
                                    isComplex = true;
                                }
                            }
                        } else {
                            if (!StringUtils.isEmpty(xmlProp.localName()) && xmlProp.localName().indexOf(':') >= 0) {
                                isComplex = true;
                            } else if (!StringUtils.isEmpty(xmlProp.namespace())) {
                                isComplex = true;
                            }
                        }
                        
                    }
                    
                    if (deser.getClass() == StringDeserializer.class) {
                        if (isComplex) {
                            skipNamespaceTags(p, true);
                        }

                        try {
                            prop.deserializeAndSet(p, ctxt, bean);

                            if (isComplex) {
                                skipNamespaceTags(p, false);
                            }
                        } catch (Exception e) {
                            wrapAndThrow(e, bean, propName, ctxt);
                        }

                        if (isWrapped) {
                            skipNamespaceTags(p, false);
                        }
                        propName = p.nextFieldName();
                    } else if (prop instanceof MethodProperty && deser.getClass() == StringCollectionDeserializer.class) {
                        List<String> valueList = new ArrayList<>();
                        String currentName = p.currentName(); 
                        while (p.currentToken() != JsonToken.END_OBJECT) {                            
                            if (isComplex) {
                                skipNamespaceTags(p, true);
                            }
                            String value = p.getValueAsString();
                            valueList.add(value);
                            if (isComplex) {
                                skipNamespaceTags(p, false);
                            }
                            if (p.nextToken() == JsonToken.FIELD_NAME && currentName.equals(p.currentName())) {
                                p.nextToken();
                                continue;
                            }
                            propName = p.currentName();
                            break;
                        }
                        ((MethodProperty) prop).set(bean, valueList);
                        
                        if (isWrapped) {
                            if (p.currentToken() == JsonToken.FIELD_NAME) {
                                do {
                                    p.nextToken();
                                    propName = p.currentName();
                                    if (propName.equals(XmlUtil.NAMESPACES_TAG)) {
                                        p.nextToken();
                                        propName = p.currentName();
                                        do {
                                            p.nextToken();
                                        } while ((propName = p.nextFieldName()) != null);
                                        continue;
                                    } else if (propName.equals(XmlUtil.NAMESPACE_PREFIX_TAG)) {
                                        continue;
                                    }
                                } while (p.nextToken() != JsonToken.END_OBJECT);
                            }
                            propName = p.nextFieldName();
                        }
                    } else {
                        try {
                            prop.deserializeAndSet(p, ctxt, bean);
                        } catch (Exception e) {
                            wrapAndThrow(e, bean, propName, ctxt);
                        }
                        
                        if (isWrapped) {
                            skipNamespaceTags(p, false);
                        }
                        propName = p.nextFieldName();
                    }
                    
                    continue;
                }
                handleUnknownVanilla(p, ctxt, bean, propName);
                propName = p.nextFieldName();
            } while (propName != null);
        }
        return bean;
    }

    private void skipNamespaceTags(JsonParser p, boolean skipPrefix) throws IOException {
        String propName = null;
        while (p.nextToken() != JsonToken.END_OBJECT) {
            propName = p.currentName();
            p.nextToken();
            if (propName.equals(XmlUtil.NAMESPACES_TAG)) {
                p.nextToken();
                propName = p.currentName();
                do {
                    p.nextToken();
                } while ((propName = p.nextFieldName()) != null);
                continue;
            } else if (propName.equals(XmlUtil.NAMESPACE_PREFIX_TAG)) {
                continue;
            } else if (skipPrefix) {
                break;
            }
        }        
    }
}
