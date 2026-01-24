package org.example.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

/** "Разбирает" объект на составные части */
public interface EntityClassMetaData<T> {
    String getName();

    Constructor<T> getConstructor() throws NoSuchMethodException;

    Field getIdField();

    List<Field> getAllFields();

    List<Field> getFieldsWithoutId();

    /**
     * Получить поле версии (если есть)
     */
    Field getVersionField();

    /**
     * Проверить, есть ли поле версии
     */
    boolean hasVersionField();

    /**
     * Получить стратегию генерации ID
     */
    GenerationStrategy getGenerationStrategy();

    /**
     * Получить имя последовательности (если используется SEQUENCE стратегия)
     */
    String getSequenceName();
}
