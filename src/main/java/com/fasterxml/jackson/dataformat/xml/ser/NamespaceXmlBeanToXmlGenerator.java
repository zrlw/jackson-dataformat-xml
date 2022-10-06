package com.fasterxml.jackson.dataformat.xml.ser;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.xml.XmlNameProcessor;
import com.fasterxml.jackson.dataformat.xml.util.StaxUtil;

/**
 * @author zrlw@sina.com
 * @date 2022/10/06
 */
public class NamespaceXmlBeanToXmlGenerator extends ToXmlGenerator {

    public NamespaceXmlBeanToXmlGenerator(IOContext ctxt, int stdFeatures, int xmlFeatures,
            ObjectCodec codec,
            XMLStreamWriter sw, XmlNameProcessor nameProcessor) {
        super(ctxt, stdFeatures, xmlFeatures, codec, sw, nameProcessor);
    }

    public String getNameSpace() {
        return _nextName.getNamespaceURI();
    }

    @Override
    public void writeString(String text) throws IOException
    {
        if (text == null) { // [dataformat-xml#413]
            writeNull();
            return;
        }
        _verifyValueWrite("write String value");
        if (_nextName == null) {
            handleMissingName();
        }
        try {
            if (_nextIsAttribute) { // must write attribute name and value with one call
                _xmlWriter.writeAttribute(_nextName.getNamespaceURI(), _nextName.getLocalPart(), text);
            } else if (checkNextIsUnwrapped()) {
                // [dataformat-xml#56] Should figure out how to prevent indentation for end element
                //   but for now, let's just make sure structure is correct
                //if (_xmlPrettyPrinter != null) { ... }
                if(_nextIsCData) {
                    _xmlWriter.writeCData(text);
                } else {
                    _xmlWriter.writeCharacters(text);
                }
            } else if (_xmlPrettyPrinter != null) {
                _xmlPrettyPrinter.writeLeafElement(_xmlWriter,
                        _nextName.getNamespaceURI(), _nextName.getLocalPart(),
                        text, _nextIsCData);
            } else {
                _xmlWriter.writeStartElement(_nextName.getPrefix(), _nextName.getLocalPart(),
                    _nextName.getNamespaceURI());
                if(_nextIsCData) {
                    _xmlWriter.writeCData(text);
                } else {
                    _xmlWriter.writeCharacters(text);
                }
                _xmlWriter.writeEndElement();
            }
        } catch (XMLStreamException e) {
            StaxUtil.throwAsGenerationException(e, this);
        }
    }

    public void writeStartNamespaceObject() throws IOException
    {
        _verifyValueWrite("start an object");
        _writeContext = _writeContext.createChildObjectContext();
        if (_cfgPrettyPrinter != null) {
            _cfgPrettyPrinter.writeStartObject(this);
        } else {
            _handleStartNamespaceObject();
        }
    }

    public void _handleStartNamespaceObject() throws IOException
    {
        if (_nextName == null) {
            handleMissingName();
        }
        // Need to keep track of names to make Lists work correctly
        _elementNameStack.addLast(_nextName);
        try {
            if (!_nextName.getPrefix().isEmpty() && !_nextName.getNamespaceURI().isEmpty()) {
                _xmlWriter.setPrefix(_nextName.getPrefix(), _nextName.getNamespaceURI());
            }
            _xmlWriter.writeStartElement(_nextName.getPrefix(), _nextName.getLocalPart(),
                _nextName.getNamespaceURI());
        } catch (XMLStreamException e) {
            StaxUtil.throwAsGenerationException(e, this);
        }
    }
    
}
