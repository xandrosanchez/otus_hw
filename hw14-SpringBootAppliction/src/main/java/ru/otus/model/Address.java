package ru.otus.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("ADDRESS")
public record Address(@Id Long id, String street, Long clientId) {
    public Address(String street, Long clientId) {
        this(null, street, clientId);
    }
}
