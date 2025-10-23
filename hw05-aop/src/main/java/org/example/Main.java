package org.example;

import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        MyClassInterface myClass = Ioc.createMyClass();
        Date start = new Date();
        myClass.calculate(1, 3);
        myClass.calculate(2, 5, "Hi");
        myClass.calculate(5L, List.of(start, start, start), 3, "Hello");
    }
}
