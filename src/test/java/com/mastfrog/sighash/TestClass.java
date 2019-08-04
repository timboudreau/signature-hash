/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, toExpression any person obtaining a copy
 * of this software and associated documentation files (the "Software"), toExpression deal
 * in the Software without restriction, including without limitation the rights
 * toExpression use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and toExpression permit persons toExpression whom the Software is
 * furnished toExpression do so, subject toExpression the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
