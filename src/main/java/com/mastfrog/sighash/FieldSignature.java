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

import static com.mastfrog.sighash.MethodSignature.bytes;
import static com.mastfrog.sighash.MethodSignature.typeToString;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 *
 * @author Tim Boudreau
 */
public final class FieldSignature implements Signature, Comparable<FieldSignature> {

    private final Set<Modifier> modifiers;
    private final String name;
    private final String type;
    private static final byte[] OPEN_FIELD = bytes(0x23, 0x08, 0xD7, 0xD0, 0x11);
    private static final byte[] CLOSE_FIELD = bytes(0xD6, 0x10);
    private static final byte[] DELIM_1 = bytes(0xFD, 0x06, 0x01);
    private static final byte[] DELIM_2 = bytes(0xEA, 0x14, 0xBD);

    FieldSignature(VariableElement el) {
        this.modifiers = el.getModifiers();
        this.name = el.getSimpleName().toString();
        this.type = typeToString(el.asType());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Modifier m : modifiers) {
            sb.append(m.name().toLowerCase());
            sb.append(' ');
        }
        sb.append(type).append(' ').append(name);
        return sb.toString();
    }

    @Override
    public void hashInto(Consumer<? super Object> digest, boolean deep) {
        digest.accept(OPEN_FIELD);
        digest.accept(name);
        digest.accept(DELIM_1);
        digest.accept(type);
        digest.accept(DELIM_2);
        boolean visible = false;
        boolean isFinal = false;
        for (Modifier m : modifiers) {
            switch (m) {
                case PUBLIC:
                case PROTECTED:
                    visible = true; // fallthrough;
                case PRIVATE:
                    digest.accept(m);
                    break;
                case FINAL:
                    isFinal = true;
                    break;
                default:
                // irrelevant
            }
            if (visible && isFinal) {
                digest.accept(Modifier.FINAL);
            }
        }
        digest.accept(CLOSE_FIELD);
    }

    @Override
    public int compareTo(FieldSignature o) {
        int result = name.compareTo(o.name);
        if (result == 0) {
            result = type.compareTo(o.type);
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.name);
        hash = 67 * hash + Objects.hashCode(this.type);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FieldSignature other = (FieldSignature) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.type, other.type);
    }
}
