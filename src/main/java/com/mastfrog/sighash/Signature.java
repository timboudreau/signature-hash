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

import java.util.function.Consumer;

/**
 * A thing which can provide objects to incorporate into a hash in a consistent
 * order over repeated runs.
 *
 * @author Tim Boudreau
 */
public interface Signature {

    public void hashInto(Consumer<? super Object> digester, boolean deep);

    /**
     * Hash the contents, not descending into method and constructor bodies.
     *
     * @param digester The consumer of objects which will hash them
     */
    default void topLevelHash(Consumer<? super Object> digester) {
        hashInto(digester, false);
    }

    /**
     * Hash the contents, descending into method bodies and resolving those
     * methods called which are available as source and incorporating their
     * bodies into the hash.  The result of this hash is far more restrictive
     * in terms of equality if sources have been modified, but still ignores
     * things like whitespace, comments and annotations that are not pertinent
     * to the method's control flow.
     *
     * @param digester A consumer of objects which will hash them
     */
    default void deepHash(Consumer<? super Object> digester) {
        hashInto(digester, true);
    }
}
