package org.example;

import org.example.annotation.After;
import org.example.annotation.Before;
import org.example.annotation.Test;

public class ExampleTest {
    private String testData;

    @Before
    public void setUp() {
        testData = "test";
        System.out.println("setUp выполняется");
    }

    @Test
    public void testSuccess() {
        assert testData.equals("test");
        System.out.println("Успешный тест выполняется");
    }

    @After
    public void tearDown() {
        testData = null;
        System.out.println("tearDown выполняется");
    }
}
