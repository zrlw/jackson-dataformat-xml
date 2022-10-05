 package com.fasterxml.jackson.dataformat.xml;

import com.fasterxml.jackson.dataformat.xml.ser.NamespaceXmlBeanSerializerModifier;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class NamespaceXmlModule extends JacksonXmlModule {

    private static final long serialVersionUID = 2707975747006459037L;

    @Override
     public void setupModule(SetupContext context)
     {
         context.addBeanSerializerModifier(new NamespaceXmlBeanSerializerModifier());
         super.setupModule(context);
     }

}
