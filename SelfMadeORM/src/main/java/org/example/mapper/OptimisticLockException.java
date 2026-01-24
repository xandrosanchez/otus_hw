package org.example.mapper;

import org.example.core.repository.DataTemplateException;

/**
 * Исключение, выбрасываемое при конфликте оптимистичной блокировки
 */
public class OptimisticLockException extends DataTemplateException {

    public OptimisticLockException(String message) {
        super(message);
    }
}
