package ru.otus.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("PHONE")
public record Phone(@Id Long id, String number, Long clientId) {
    public Phone(String number, Long clientId) {
        this(null, number, clientId);
    }
}
