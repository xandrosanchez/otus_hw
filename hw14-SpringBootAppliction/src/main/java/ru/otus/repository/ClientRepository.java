package ru.otus.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import ru.otus.model.Client;

public interface ClientRepository extends CrudRepository<Client, Long> {

    List<Client> findByNameContainingIgnoreCase(String name);
}
