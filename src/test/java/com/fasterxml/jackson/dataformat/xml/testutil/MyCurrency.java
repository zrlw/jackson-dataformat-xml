 package com.fasterxml.jackson.dataformat.xml.testutil;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
public class MyCurrency {
     @JacksonXmlProperty(localName = "Ccy", isAttribute = true)
     private String ccy;
     
     @JacksonXmlText
     private String amt;
     
     public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy;
    }

    public String getAmt() {
        return amt;
    }

    public void setAmt(String amt) {
        this.amt = amt;
    }

    @Override
     public int hashCode() {
         int hashcode = 0;
         if (ccy != null) {
             hashcode = ccy.hashCode() * 31;
         }
         
         if (amt != null) {
             hashcode += amt.hashCode();
         }
         
         return hashcode;
     }
     
     @Override
     public boolean equals(Object o) {
         if (o == null || !(o instanceof MyCurrency)) {
             return false;
         }
         
         MyCurrency my1 = (MyCurrency) o;
         if (my1.ccy == null) {
             if (this.ccy != null) {
                 return false;
             }
         } else if (!my1.ccy.equals(this.ccy)) {
             return false;
         }
         
         if (my1.amt == null) {
             if (this.amt != null) {
                 return false;
             }
         }
         return my1.amt.equals(this.amt);
     }

}
