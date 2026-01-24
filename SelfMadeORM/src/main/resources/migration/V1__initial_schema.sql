create table test
(
    id   int,
    name varchar(50)
);
-- PostgreSQL создает таблицы в lowercase автоматически
CREATE TABLE IF NOT EXISTS client
(
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    version BIGINT       NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS manager
(
    no      BIGINT PRIMARY KEY,
    label   VARCHAR(255) NOT NULL,
    param1  VARCHAR(255),
    version BIGINT       NOT NULL DEFAULT 0
);


