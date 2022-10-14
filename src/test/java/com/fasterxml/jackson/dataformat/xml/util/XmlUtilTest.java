 package com.fasterxml.jackson.dataformat.xml.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.testutil.MyCurrency;
import com.fasterxml.jackson.dataformat.xml.testutil.MyEnvelope;
import com.fasterxml.jackson.dataformat.xml.testutil.MyPacketBody;
import com.fasterxml.jackson.dataformat.xml.testutil.MyStudent;

import junit.framework.TestCase;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class XmlUtilTest extends TestCase {

     public void testXmlUtil() throws Exception {
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
        String envelopeXml = XmlUtil.objectToXml(myEnvelope);
        myEnvelope1 = XmlUtil.xmlToObject(envelopeXml, myTypeReference);
        Assert.assertEquals(myEnvelope, myEnvelope1);

        Map<String, Object> map = XmlUtil.xmlToObjectMap(envelopeXml);
        myEnvelope1 = XmlUtil.objectMapToObject(map, myTypeReference);
        Assert.assertEquals(myEnvelope, myEnvelope1);

        map = XmlUtil.objectToObjectMap(myEnvelope);
        myEnvelope1 = XmlUtil.objectMapToObject(map, myTypeReference);
        Assert.assertEquals(myEnvelope, myEnvelope1);

        myEnvelope1 = XmlUtil.xmlToObject(envelopeXml, myTypeReference);
        Assert.assertEquals(myEnvelope, myEnvelope1);

        myEnvelope1 = XmlUtil.objectMapToObject(map, myTypeReference);
        Assert.assertEquals(myEnvelope, myEnvelope1);

        byte[] bytes;
        bytes = XmlUtil.objectToBytes(myEnvelope);
        myEnvelope1 = XmlUtil.bytesToObject(bytes, myTypeReference);
        Assert.assertTrue(myEnvelope.equals(myEnvelope1));
     }

}
