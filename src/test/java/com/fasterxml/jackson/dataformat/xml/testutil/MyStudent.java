 package com.fasterxml.jackson.dataformat.xml.testutil;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author zrlw@sina.com
 * @date 2022/10/04
 */
@JacksonXmlRootElement(localName = "Student", namespace = "http://my.com")
public class MyStudent {
    private String name;
    
    @JacksonXmlProperty(localName = "SchoolName")
    private String school;
    
    @JacksonXmlProperty(localName = "xyz:FeeA", namespace = "http://xyz")
    private MyCurrency fee1;
    
    @JacksonXmlProperty(localName = "soap:FeeB")
    private MyCurrency fee2;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MyCurrency getFee1() {
        return fee1;
    }

    public void setFee1(MyCurrency fee) {
        this.fee1 = fee;
    }

    public MyCurrency getFee2() {
        return fee2;
    }

    public void setFee2(MyCurrency fee) {
        this.fee2 = fee;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    @Override
    public int hashCode() {
        int hashcode = 0;
        if (name != null) {
            hashcode = name.hashCode() * 31 * 31 * 31;
        }
        
        if (school != null) {
            hashcode += school.hashCode() * 31 * 31;
        }

        if (fee1 != null) {
            hashcode = fee1.hashCode() * 31;
        }
        
        if (fee2 != null) {
            hashcode = fee2.hashCode();
        }
        
        return hashcode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof MyStudent)) {
            return false;
        }
        
        MyStudent my1 = (MyStudent) o;
        if (my1.name == null) {
            if (this.name != null) {
                return false;
            }
        } else if (!my1.name.equals(this.name)) {
            return false;
        }

        if (my1.school == null) {
            if (this.school != null) {
                return false;
            }
        } else if (!my1.school.equals(this.school)) {
            return false;
        }
        
        if (my1.fee1 == null) {
            if (this.fee1 != null) {
                return false;
            }
        } else if (!my1.fee1.equals(this.fee1)) {
            return false;
        }

        if (my1.fee2 == null) {
            if (this.fee2 != null) {
                return false;
            }
        }
        return my1.fee2.equals(this.fee2);
    }

}
