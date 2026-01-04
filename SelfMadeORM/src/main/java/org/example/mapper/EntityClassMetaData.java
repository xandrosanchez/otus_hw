package org.example.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

/** "Разбирает" объект на составные части */
public interface EntityClassMetaData<T> {
    String getName();

    Constructor<T> getConstructor() throws NoSuchMethodException;

    /**
     * Поле Id должно определять по наличию аннотации Id
     * @return поле с аннотацией @Id
     * @throws IllegalStateException если поле с @Id не найдено
     */
    Field getIdField();

    List<Field> getAllFields();

    List<Field> getFieldsWithoutId();

    /**
     * Получить стратегию генерации ID
     * @return стратегия генерации, может быть null если нет @GeneratedValue
     */
    GenerationStrategy getGenerationStrategy();

    /**
     * Получить имя последовательности (если используется SEQUENCE стратегия)
     * @return имя последовательности или пустая строка
     */
    String getSequenceName();
}
