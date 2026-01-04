package org.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для обозначения стратегии генерации значения ID
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GeneratedValue {
    /**
     * Стратегия генерации значения
     */
    GenerationType strategy() default GenerationType.IDENTITY;

    /**
     * Имя последовательности (используется для стратегии SEQUENCE)
     */
    String sequenceName() default "";
}
