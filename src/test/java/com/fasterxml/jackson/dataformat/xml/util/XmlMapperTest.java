package com.fasterxml.jackson.dataformat.xml.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.NamespaceXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.testutil.MyCurrency;
import com.fasterxml.jackson.dataformat.xml.testutil.MyEnvelope;
import com.fasterxml.jackson.dataformat.xml.testutil.MyPacketBody;
import com.fasterxml.jackson.dataformat.xml.testutil.MyStudent;

import junit.framework.TestCase;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class XmlMapperTest extends TestCase {

    private JacksonXmlModule jacksonXmlModule = null;
    private XmlMapper xmlMapper = null;
    {
        jacksonXmlModule = new JacksonXmlModule();
        jacksonXmlModule.setXMLTextElementName("_text");
        xmlMapper = new XmlMapper(jacksonXmlModule);
        xmlMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        xmlMapper.setAnnotationIntrospector(new NamespaceXmlAnnotationIntrospector());
    }

    public void testXmlMapper1() throws Exception {
        MyCurrency myFee1 = new MyCurrency();
        myFee1.setCcy("USD");
        myFee1.setAmt("1.00");
        MyCurrency myFee2 = new MyCurrency();
        myFee2.setCcy("EUR");
        myFee2.setAmt("2.00");
        MyStudent myStudent = new MyStudent();
        myStudent.setName("nameA");
        List<String> schools = new ArrayList<>();
        schools.add("schoolA");
        schools.add("schoolB");
        myStudent.setSchools(schools);
        myStudent.setFee1(myFee1);
        myStudent.setFee2(myFee2);

        String envelopeXml = xmlMapper.writeValueAsString(myStudent);

        TypeReference<Map<String, Object>> mapReference = new TypeReference<Map<String, Object>>() {};
        Map<String, Object> map = xmlMapper.readValue(envelopeXml, mapReference);

        byte[] mapBytes = xmlMapper.writeValueAsBytes(map);
        MyStudent myStudent1 = xmlMapper.readValue(mapBytes, MyStudent.class);
        Assert.assertEquals(myStudent, myStudent1);

        byte[] objectBytes = xmlMapper.writeValueAsBytes(myStudent1);
        Map<String, Object> map1 = xmlMapper.readValue(objectBytes, mapReference);
        byte[] mapBytes1 = xmlMapper.writeValueAsBytes(map1);
        myStudent1 = xmlMapper.readValue(mapBytes1, MyStudent.class);
        Assert.assertEquals(myStudent, myStudent1);
    }
    public void testXmlMapper2() throws Exception {
        MyEnvelope<MyStudent> myEnvelope = new MyEnvelope<MyStudent>();
        MyCurrency myFee1 = new MyCurrency();
        myFee1.setCcy("USD");
        myFee1.setAmt("1.00");
        MyCurrency myFee2 = new MyCurrency();
        myFee2.setCcy("EUR");
        myFee2.setAmt("2.00");
        MyStudent myStudent = new MyStudent();
        myStudent.setName("nameA");
        List<String> schools = new ArrayList<>();
        schools.add("schoolA");
        schools.add("schoolB");
        myStudent.setSchools(schools);
        myStudent.setFee1(myFee1);
        myStudent.setFee2(myFee2);
        MyPacketBody<MyStudent> myBody = new MyPacketBody<MyStudent>();
        myBody.setT(myStudent);
        myEnvelope.setTitle("titleA");
        myEnvelope.setBody(myBody);

        TypeReference<MyEnvelope<MyStudent>> myTypeReference = new TypeReference<MyEnvelope<MyStudent>>() {};

        MyEnvelope<MyStudent> myEnvelope1 = null;
        String envelopeXml = xmlMapper.writeValueAsString(myEnvelope);
        myEnvelope1 = xmlMapper.readValue(envelopeXml, myTypeReference);
        Assert.assertEquals(myEnvelope, myEnvelope1);

        TypeReference<Map<String, Object>> mapReference = new TypeReference<Map<String, Object>>() {};
        Map<String, Object> map = xmlMapper.readValue(envelopeXml, mapReference);
        byte[] mapBytes = xmlMapper.writeValueAsBytes(map);
        myEnvelope1 = xmlMapper.readValue(mapBytes, myTypeReference);
        Assert.assertEquals(myEnvelope, myEnvelope1);

        byte[] objectBytes = xmlMapper.writeValueAsBytes(myEnvelope);
        map = xmlMapper.readValue(objectBytes, mapReference);
        mapBytes = xmlMapper.writeValueAsBytes(map);
        myEnvelope1 = xmlMapper.readValue(mapBytes, myTypeReference);
        Assert.assertEquals(myEnvelope, myEnvelope1);

        myEnvelope1 = xmlMapper.readValue(envelopeXml, myTypeReference);
        Assert.assertEquals(myEnvelope, myEnvelope1);

        myEnvelope1 = xmlMapper.readValue(mapBytes, myTypeReference);
        Assert.assertEquals(myEnvelope, myEnvelope1);

        byte[] bytes;
        bytes = XmlUtil.objectToBytes(myEnvelope);
        myEnvelope1 = XmlUtil.bytesToObject(bytes, myTypeReference);
        Assert.assertTrue(myEnvelope.equals(myEnvelope1));
    }

}
