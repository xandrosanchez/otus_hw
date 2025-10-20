package org.example;

import java.util.Date;
import java.util.List;

public class MyClassImpl implements MyClassInterface {

    @Override
    @Log
    public void calculate(int a, int b) {}

    @Override
    @Log
    public void calculate(int a, int b, String c) {}

    @Override
    @Log
    public void calculate(Long a, List<Date> dates, int c, String d) {}
}
