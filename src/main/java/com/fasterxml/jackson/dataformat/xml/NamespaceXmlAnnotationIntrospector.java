 package com.fasterxml.jackson.dataformat.xml;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class NamespaceXmlAnnotationIntrospector extends JacksonXmlAnnotationIntrospector {

    private static final long serialVersionUID = 4174923868707107806L;

    @Override
    public PropertyName findWrapperName(Annotated ann) {
        PropertyName propertyName = super.findWrapperName(ann);
        return removeNamespacePrefix(propertyName);
    }

    @Override
    public PropertyName findRootName(AnnotatedClass ac) {
        PropertyName propertyName = super.findRootName(ac);
        return removeNamespacePrefix(propertyName);
    }

    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        PropertyName propertyName = super.findNameForSerialization(a);
        return removeNamespacePrefix(propertyName);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        PropertyName propertyName = super.findNameForDeserialization(a);
        return removeNamespacePrefix(propertyName);
    }

    @Override
    protected PropertyName _findXmlName(Annotated a)
    {
        PropertyName propertyName = super._findXmlName(a);
        return removeNamespacePrefix(propertyName);
    }

    private PropertyName removeNamespacePrefix(PropertyName propertyName) {
        if (propertyName == null) {
            return null;
        }
        String simpleName = propertyName.getSimpleName();
        if (simpleName != null && simpleName.length() > 0) {
            int idx = simpleName.indexOf(':');
            if (idx >= 0) {
                simpleName = simpleName.substring(idx + 1);
                propertyName = propertyName.withSimpleName(simpleName);
            }
        }
        return propertyName;
    }

}
