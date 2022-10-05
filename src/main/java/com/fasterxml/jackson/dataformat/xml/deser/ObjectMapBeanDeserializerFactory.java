 package com.fasterxml.jackson.dataformat.xml.deser;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.dataformat.xml.util.XmlUtil;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class ObjectMapBeanDeserializerFactory extends BeanDeserializerFactory {

    private static final long serialVersionUID = -6224577427973469796L;

    public final static BeanDeserializerFactory instance = new ObjectMapBeanDeserializerFactory(
        new DeserializerFactoryConfig()
            .withDeserializerModifier(new XmlBeanDeserializerModifier(XmlUtil.COMPLEX_NODE_TEXT_TAG)));
    
    public ObjectMapBeanDeserializerFactory(DeserializerFactoryConfig config) {
        super(config);
    }

    @Override
    protected BeanDeserializerBuilder constructBeanDeserializerBuilder(DeserializationContext ctxt,
            BeanDescription beanDesc) {
        return new ObjectMapBeanDeserializerBuilder(beanDesc, ctxt);
    }
}
