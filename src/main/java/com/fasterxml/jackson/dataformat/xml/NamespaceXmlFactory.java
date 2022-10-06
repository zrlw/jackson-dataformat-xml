 package com.fasterxml.jackson.dataformat.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.xml.ser.NamespaceXmlBeanToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/**
 * @author zrlw@sina.com
 * @date 2022/10/06
 */
public class NamespaceXmlFactory extends XmlFactory {

    private static final long serialVersionUID = 9095115058318626408L;

    public NamespaceXmlFactory() {
         super();
    }

    @Override
    public ToXmlGenerator createGenerator(File f, JsonEncoding enc) throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        final IOContext ctxt = _createContext(_createContentReference(out), true);
        ctxt.setEncoding(enc);
        return new NamespaceXmlBeanToXmlGenerator(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(ctxt, out), _nameProcessor);
    }

    @Override
    public ToXmlGenerator createGenerator(OutputStream out) throws IOException {
        return createGenerator(out, JsonEncoding.UTF8);
    }

    @Override
    public ToXmlGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        final IOContext ctxt = _createContext(_createContentReference(out), false);
        ctxt.setEncoding(enc);
        return new NamespaceXmlBeanToXmlGenerator(ctxt,
                _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(ctxt, out), _nameProcessor);
    }

    @Override
    public ToXmlGenerator createGenerator(Writer out) throws IOException
    {
        final IOContext ctxt = _createContext(_createContentReference(out), false);
        return new NamespaceXmlBeanToXmlGenerator(ctxt,
                _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, _createXmlWriter(ctxt, out), _nameProcessor);
    }

    public ToXmlGenerator createGenerator(XMLStreamWriter sw) throws IOException
    {
        sw = _initializeXmlWriter(sw);
        IOContext ctxt = _createContext(_createContentReference(sw), false);
        return new NamespaceXmlBeanToXmlGenerator(ctxt, _generatorFeatures, _xmlGeneratorFeatures,
                _objectCodec, sw, _nameProcessor);
    }

}
