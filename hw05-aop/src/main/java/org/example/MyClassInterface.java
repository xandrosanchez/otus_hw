package org.example;

import java.util.Date;
import java.util.List;

public interface MyClassInterface {
    void calculate(int a, int b);
    void calculate(int a, int b, String c);
    void calculate(Long a, List<Date> dates, int c, String d);
}
