-- Создаем таблицу для Product с UUID
CREATE TABLE IF NOT EXISTS product
(
    id      UUID PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    price   DECIMAL(10, 2),
    version BIGINT       NOT NULL DEFAULT 0
);
-- Создаем индекс для быстрого поиска по имени
CREATE INDEX IF NOT EXISTS idx_product_name ON product (name);