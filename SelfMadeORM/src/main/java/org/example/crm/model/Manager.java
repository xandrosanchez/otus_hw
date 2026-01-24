package org.example.crm.model;

import org.example.GeneratedValue;
import org.example.GenerationType;
import org.example.Id;
import org.example.Version;

public class Manager {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, sequenceName = "manager_seq")
    private Long no;

    private String label;

    private String param1;

    @Version
    private Long version;

    public Manager() {
        this.version = 0L;
    }

    public Manager(String label) {
        this.label = label;
        this.version = 0L;
    }

    public Manager(Long no, String label, String param1) {
        this.no = no;
        this.label = label;
        this.param1 = param1;
        this.version = 0L;
    }

    public Manager(Long no, String label, String param1, Long version) {
        this.no = no;
        this.label = label;
        this.param1 = param1;
        this.version = version;
    }

    public Long getNo() {
        return no;
    }

    public void setNo(Long no) {
        this.no = no;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getParam1() {
        return param1;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Manager{" + "no=" + no + ", label='" + label + '\'' + ", version=" + version + '}';
    }
}
