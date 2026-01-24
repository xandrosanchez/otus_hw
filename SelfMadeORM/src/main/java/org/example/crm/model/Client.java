package org.example.crm.model;

import org.example.GeneratedValue;
import org.example.GenerationType;
import org.example.Id;
import org.example.Version;

public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Version
    private Long version;

    public Client() {
        this.version = 0L; // Начальная версия
    }

    public Client(String name) {
        this.id = null;
        this.name = name;
        this.version = 0L;
    }

    public Client(Long id, String name) {
        this.id = id;
        this.name = name;
        this.version = 0L;
    }

    public Client(Long id, String name, Long version) {
        this.id = id;
        this.name = name;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getVersion() {
        return version;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Client{" + "id=" + id + ", name='" + name + '\'' + ", version=" + version + '}';
    }
}
