package org.example.crm.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.crm.model.Product;

public interface DBServiceProduct {

    Product saveProduct(Product product);

    Optional<Product> getProduct(UUID id);

    List<Product> findAll();
}
