-- Создаем последовательность для Manager
CREATE SEQUENCE IF NOT EXISTS manager_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;