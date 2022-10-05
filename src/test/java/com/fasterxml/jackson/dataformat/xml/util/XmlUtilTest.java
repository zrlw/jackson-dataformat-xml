 package com.fasterxml.jackson.dataformat.xml.util;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

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
         myStudent.setSchool("schoolA");
         myStudent.setFee1(myFee1);
         myStudent.setFee2(myFee2);
         MyPacketBody<MyStudent> myBody = new MyPacketBody<MyStudent>();
         myBody.setT(myStudent);
         myEnvelope.setTitle("titleA");
         myEnvelope.setBody(myBody);
 
         TypeReference<MyEnvelope<MyStudent>> myTypeReference = new TypeReference<MyEnvelope<MyStudent>>() {};

         // parallel testing.
         int cnt = 5;
         CountDownLatch latch = new CountDownLatch(cnt * 3);
         for (int i = 0; i < cnt; i++) {
             new Thread( () -> {
                 MyEnvelope<MyStudent> myEnvelope1 = null;
                 try {
                     String envelopeXml = XmlUtil.objectToXml(myEnvelope);
                     myEnvelope1 = XmlUtil.xmlToObject(envelopeXml, myTypeReference);
                     Assert.assertEquals(myEnvelope, myEnvelope1);
                     latch.countDown();
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }).start();

             new Thread( () -> {
                 MyEnvelope<MyStudent> myEnvelope1 = null;
                 try {
                     Map<String, Object> map = XmlUtil.objectToObjectMap(myEnvelope);
                     myEnvelope1 = XmlUtil.objectMapToObject(map, myTypeReference);
                     Assert.assertEquals(myEnvelope, myEnvelope1);

                     String envelopeXml = XmlUtil.objectMapToXml(map);
                     myEnvelope1 = XmlUtil.xmlToObject(envelopeXml, myTypeReference);
                     Assert.assertEquals(myEnvelope, myEnvelope1);

                     myEnvelope1 = XmlUtil.objectMapToObject(map, myTypeReference);
                     Assert.assertEquals(myEnvelope, myEnvelope1);
                     latch.countDown();
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }).start();
             
             new Thread( () -> {
                 byte[] bytes;
                 MyEnvelope<MyStudent> myEnvelope1 = null;
                 try {
                     bytes = XmlUtil.objectToBytes(myEnvelope);
                     myEnvelope1 = XmlUtil.bytesToObject(bytes, myTypeReference);
                     Assert.assertTrue(myEnvelope.equals(myEnvelope1));
                     latch.countDown();
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }).start();
         }
         
         latch.await();
     }

}
