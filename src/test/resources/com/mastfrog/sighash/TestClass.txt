package com.mastfrog.sighash;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestClass<T extends Enum<T> & CharSequence> {

    public final Class<T> type;
    private int total;
    private final Map<String, Integer> values = new HashMap<>();
    public final StringBuilder sb = new StringBuilder();
    public final OtherTestClass other = new OtherTestClass();

    public TestClass(Class<T> type) {
        this.type = type;
    }

    public T get(CharSequence seq) {
        for (T ec : type.getEnumConstants()) {

        }
        return null;
    }

    public void add(int val, String s) {
        values.put(s, val);
        total = add(total, val);
    }

    private int add(int a, int b) {
        return other.add(a, b);
    }

    public void doSomething(String s) {
        sb.append(total).append("hello");
    }

    public List<String> doSomethingMore(List<String> l) {
        for (String s : l) {
            sb.append(s);
        }
        return l;
    }

    public void doSomethingElse(List<? extends String> l) {

    }

    public enum Foo {
        BAR, BAZ, QUUX
    }
}
