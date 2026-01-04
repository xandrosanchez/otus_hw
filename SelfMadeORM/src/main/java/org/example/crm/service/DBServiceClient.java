package org.example.crm.service;

import java.util.List;
import java.util.Optional;
import org.example.crm.model.Client;

public interface DBServiceClient {

    Client saveClient(Client client);

    Optional<Client> getClient(long id);

    List<Client> findAll();
}
