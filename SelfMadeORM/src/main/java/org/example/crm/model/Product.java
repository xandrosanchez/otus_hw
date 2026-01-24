package org.example.crm.model;

import java.util.UUID;
import org.example.GeneratedValue;
import org.example.GenerationType;
import org.example.Id;
import org.example.Version;

public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private Double price;

    @Version
    private Long version;

    public Product() {
        this.version = 0L;
    }

    public Product(String name, Double price) {
        this.id = null;
        this.name = name;
        this.price = price;
        this.version = 0L;
    }

    public Product(UUID id, String name, Double price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.version = 0L;
    }

    public Product(UUID id, String name, Double price, Long version) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Product{" + "id=" + id + ", name='" + name + '\'' + ", price=" + price + ", version=" + version + '}';
    }
}
