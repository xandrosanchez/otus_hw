package org.example;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class HelloOtus {
    public static void main(String[] args) {
        Multiset<String> words = HashMultiset.create();
        words.add("hello");
        words.forEach(System.out::println);
    }
}
