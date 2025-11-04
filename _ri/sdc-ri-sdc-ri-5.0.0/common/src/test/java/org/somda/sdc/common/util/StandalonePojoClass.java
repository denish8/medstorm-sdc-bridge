package org.somda.sdc.common.util;

import java.util.ArrayList;
import java.util.List;

public class StandalonePojoClass {
    private String str;
    private int num;
    private List<String> strList;

    public StandalonePojoClass() {
        this.str = "";
        this.num = 0;
        this.strList = new ArrayList<>();
    }

    public StandalonePojoClass(String str, int num, List<String> strList) {
        this.str = str;
        this.num = num;
        this.strList = strList;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public List<String> getStrList() {
        return strList;
    }

    public void setStrList(List<String> strList) {
        this.strList = strList;
    }
}
