 package com.fasterxml.jackson.dataformat.xml;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class NamespaceXmlAnnotationIntrospector extends JacksonXmlAnnotationIntrospector {

    private static final long serialVersionUID = 4174923868707107806L;

    @Override
     public PropertyName findWrapperName(Annotated ann)
     {
         PropertyName propertyName = super.findWrapperName(ann);
         return removeNamespacePrefix(propertyName);
     }

     @Override
     public PropertyName findNameForDeserialization(Annotated a)
     {
         PropertyName propertyName = super.findNameForDeserialization(a);
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
