package org.example.crm.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.core.repository.DataTemplate;
import org.example.core.sessionmanager.TransactionRunner;
import org.example.crm.model.Product;
import org.example.crm.service.DBServiceProduct;
import org.example.mapper.DataTemplateJdbc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbServiceProductImpl implements DBServiceProduct {
    private static final Logger log = LoggerFactory.getLogger(DbServiceProductImpl.class);

    private final DataTemplate<Product> dataTemplate;
    private final TransactionRunner transactionRunner;

    public DbServiceProductImpl(TransactionRunner transactionRunner, DataTemplate<Product> dataTemplate) {
        this.transactionRunner = transactionRunner;
        this.dataTemplate = dataTemplate;
    }

    @Override
    public Product saveProduct(Product product) {
        return transactionRunner.doInTransaction(connection -> {
            // Для UUID стратегии всегда выполняем insert, так как ID генерируется сразу
            if (product.getId() == null) {
                var result = dataTemplate.insert(connection, product);
                log.info("created product: {}", product);
                return product;
            } else {
                // Пытаемся обновить, но если это новый объект с предустановленным UUID
                // нужно сначала проверить, существует ли он
                try {
                    dataTemplate.update(connection, product);
                    log.info("updated product: {}", product);
                    return product;
                } catch (Exception e) {
                    // Если объект не найден для обновления, значит это новый объект
                    // с предустановленным UUID
                    var result = dataTemplate.insert(connection, product);
                    log.info("created product with predefined UUID: {}", product);
                    return product;
                }
            }
        });
    }

    @Override
    public Optional<Product> getProduct(UUID id) {
        return transactionRunner.doInTransaction(connection -> {
            // Используем специальный метод для UUID, если доступен
            if (dataTemplate instanceof DataTemplateJdbc) {
                DataTemplateJdbc<Product> template = (DataTemplateJdbc<Product>) dataTemplate;
                var productOptional = template.findByUuid(connection, id);
                log.info("product: {}", productOptional);
                return productOptional;
            } else {
                // Fallback: используем стандартный метод findById
                // Но он работает только с long, а у нас UUID
                throw new UnsupportedOperationException("This DataTemplate doesn't support UUID search");
            }
        });
    }

    @Override
    public List<Product> findAll() {
        return transactionRunner.doInTransaction(connection -> {
            var productList = dataTemplate.findAll(connection);
            log.info("productList:{}", productList);
            return productList;
        });
    }
}
