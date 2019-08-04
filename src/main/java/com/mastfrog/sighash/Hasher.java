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

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collection;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
public class Hasher implements Consumer<Object> {

    private final MessageDigest digest;
    private byte[] finalDigest;

    public Hasher(MessageDigest digest) {
        this.digest = digest;
    }

    public String toString() {
        return Base64.getUrlEncoder().encodeToString(done());
    }

    @Override
    public void accept(Object t) {
        add(t);
    }

    public byte[] done() {
        if (finalDigest == null) {
            return finalDigest = digest.digest();
        }
        return finalDigest;
    }

    private String stringify(Object o) {
        String stringRep;
        if (o instanceof CharSequence) {
            stringRep = o.toString().trim();
        } else if (o instanceof Enum<?>) {
            Enum<?> e = (Enum<?>) o;
            stringRep = e.name() + ":" + e.getClass().getSimpleName();
        } else if (o instanceof Iterable<?>) {
            StringBuilder sb = new StringBuilder(512).append('[');
            for (Object o1 : ((Iterable<?>) o)) {
                sb.append(stringify(o1)).append(' ');
            }
            sb.append(']');
            stringRep = sb.toString();
        } else if (o == null) {
            stringRep = "null";
        } else {
            stringRep = o.toString();
        }
        return stringRep;
    }

    private static ByteBuffer intToByteBuffer(int val) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
        buf.putInt(val);
        buf.flip();
        return buf;
    }

    private int lengthValue(Object o) {
        if (o instanceof CharSequence) {
            return ((CharSequence) o).length();
        } else if (o instanceof Collection<?>) {
            return ((Collection<?>) o).size();
        } else if (o != null && o.getClass().isArray()) {
            return Array.getLength(o);
        } else {
            return -1;
        }
    }

    void add(Object o) {
        assert finalDigest == null;
        if (o instanceof byte[]) {
            digest.update((byte[]) o);
        } else {
            int lv = lengthValue(o);
            if (lv > 0) {
                digest.update(intToByteBuffer(-lv));
            }
            String stringRep = stringify(o);
//            System.out.println(" - " + stringRep);
            int hashCode = stringRep.hashCode();
            digest.update(intToByteBuffer(hashCode));
            digest.update(stringRep.getBytes(UTF_8));
        }
    }
}
