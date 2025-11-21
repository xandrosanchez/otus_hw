package ru.otus.crm.service;

public record ClientId(long id) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientId clientId = (ClientId) o;
        return id == clientId.id;
    }
}
