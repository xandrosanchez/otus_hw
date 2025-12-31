package ru.otus.dto;

import java.util.ArrayList;
import java.util.List;

public class ClientDto {
    private Long id;
    private String name;
    private String address;
    private List<String> phones = new ArrayList<>();

    public ClientDto() {}

    public ClientDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getPhones() {
        return phones;
    }

    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    public void addPhone(String phone) {
        this.phones.add(phone);
    }
}
