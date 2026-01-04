package org.example.crm.model;

import java.util.UUID;
import org.example.GeneratedValue;
import org.example.GenerationType;
import org.example.Id;

public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private Double price;

    public Product() {}

    public Product(String name, Double price) {
        this.id = null; // Будет сгенерирован при сохранении
        this.name = name;
        this.price = price;
    }

    public Product(UUID id, String name, Double price) {
        this.id = id;
        this.name = name;
        this.price = price;
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

    @Override
    public String toString() {
        return "Product{" + "id=" + id + ", name='" + name + '\'' + ", price=" + price + '}';
    }
}
