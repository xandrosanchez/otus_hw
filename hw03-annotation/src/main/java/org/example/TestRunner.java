package org.example;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.example.annotation.After;
import org.example.annotation.Before;
import org.example.annotation.Test;

public class TestRunner {
    private static class TestResult {
        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;

        void incrementTotal() {
            totalTests++;
        }

        void incrementPassed() {
            passedTests++;
        }

        void incrementFailed() {
            failedTests++;
        }
    }

    // Получаем все методы с аннотацией @Test

    /**
     * Получаем все методы с аннотацией @Test
     */
    private static List<Method> getTestMethods(Class<?> clazz) {
        List<Method> testMethods = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Test.class)) {
                testMethods.add(method);
            }
        }
        return testMethods;
    }

    /**
     * Получаем все методы с аннотацией @Before
     */
    private static List<Method> getBeforeMethods(Class<?> clazz) {
        List<Method> beforeMethods = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Before.class)) {
                beforeMethods.add(method);
            }
        }
        return beforeMethods;
    }

    /**
     * Получаем все методы с аннотацией @After
     */
    private static List<Method> getAfterMethods(Class<?> clazz) {
        List<Method> afterMethods = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(After.class)) {
                afterMethods.add(method);
            }
        }
        return afterMethods;
    }

    private static void executeTestMethod(
            Object testInstance,
            Method testMethod,
            List<Method> beforeMethods,
            List<Method> afterMethods,
            TestResult result) {
        try {
            // Выполняем @Before методы
            for (Method beforeMethod : beforeMethods) {
                beforeMethod.invoke(testInstance);
            }

            // Выполняем сам тест
            testMethod.invoke(testInstance);
            result.incrementPassed();
        } catch (Exception e) {
            System.err.println("Ошибка в тесте " + testMethod.getName() + ": " + e.getMessage());
            result.incrementFailed();
        } finally {
            // Выполняем @After методы
            for (Method afterMethod : afterMethods) {
                try {
                    afterMethod.invoke(testInstance);
                } catch (Exception e) {
                    System.err.println("Ошибка в @After методе " + afterMethod.getName() + ": " + e.getMessage());
                }
            }
        }
        result.incrementTotal();
    }

    // Статический метод для запуска тестов
    public static void run(Class<?> clazz) {
        try {
            TestResult result = new TestResult();

            Constructor<?> constructor = clazz.getConstructor();
            List<Method> testMethods = getTestMethods(clazz);
            List<Method> beforeMethods = getBeforeMethods(clazz);
            List<Method> afterMethods = getAfterMethods(clazz);

            // Делаем методы доступными через reflection
            for (Method method : beforeMethods) method.setAccessible(true);
            for (Method method : testMethods) method.setAccessible(true);
            for (Method method : afterMethods) method.setAccessible(true);

            // Выполняем каждый тестовый метод в отдельном экземпляре
            for (Method testMethod : testMethods) {
                Object testInstance = constructor.newInstance();
                executeTestMethod(testInstance, testMethod, beforeMethods, afterMethods, result);
            }

            // Выводим статистику
            System.out.println("\nСтатистика тестирования:");
            System.out.println("------------------------");
            System.out.println("Всего тестов: " + result.totalTests);
            System.out.println("Успешных: " + result.passedTests);
            System.out.println("Проваленных: " + result.failedTests);

        } catch (Exception e) {
            System.err.println("Ошибка при выполнении тестов: " + e.getMessage());
        }
    }
}
