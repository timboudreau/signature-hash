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
import static com.mastfrog.sighash.MethodSignature.compareCollections;
import static com.mastfrog.sighash.MethodSignature.sort;
import static com.mastfrog.sighash.MethodSignature.typeToString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author Tim Boudreau
 */
public final class ClassSignature implements Signature, Comparable<ClassSignature> {

    private final ElementKind kind;
    private final String name;
    private final NestingKind nestingKind;
    private final String supertype;
    private final List<String> typeParams;
    private final List<MethodSignature> methods = new ArrayList<>();
    private final List<FieldSignature> fields = new ArrayList<>();
    private final List<String> ifaces;
    private static final byte[] OPEN_CLASS = bytes(0xFF, 0xF3, 0xE2);
    private static final byte[] CLOSE_CLASS = bytes(0xFE, 0xF2, 0xE1);
    private static final byte[] DELIM = bytes(0x00, 0xFD, 0X0A);
    private static final byte[] DELIM1 = bytes(0x01, 0xFC, 0X0B);
    private static final byte[] DELIM2 = bytes(0x00, 0xFB);
    private static final byte[] DELIM3 = bytes(0xD2, 0xDA);

    ClassSignature(TypeElement el) {
        kind = el.getKind();
        name = el.getQualifiedName().toString();
        nestingKind = el.getNestingKind();
        List<? extends TypeParameterElement> params = el.getTypeParameters();
        if (!params.isEmpty()) {
            typeParams = new ArrayList<>();
            for (TypeParameterElement typeParam : params) {
                typeParams.add(typeToString(typeParam.asType()));
            }
        } else {
            typeParams = null;
        }
        List<? extends TypeMirror> tms = el.getInterfaces();
        if (!tms.isEmpty()) {
            ifaces = new ArrayList<>();
            for (TypeMirror tm : tms) {
                ifaces.add(typeToString(tm));
            }
        } else {
            ifaces = null;
        }
        supertype = typeToString(el.getSuperclass());
    }

    public Iterable<? extends FieldSignature> fields() {
        return Collections.unmodifiableCollection(fields);
    }

    public Iterable<? extends MethodSignature> methods() {
        return Collections.unmodifiableCollection(methods);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(kind.name().toLowerCase()).append(' ');
        sb.append(name);
        if (typeParams != null) {
            sb.append('<');
            for (Iterator<String> it = typeParams.iterator(); it.hasNext();) {
                String tp = it.next();
                sb.append(tp);
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append('>');
        }
        sb.append(" extends ").append(supertype);
        if (ifaces != null) {
            sb.append(" implements ");
            for (Iterator<String> it = ifaces.iterator(); it.hasNext();) {
                String ifa = it.next();
                sb.append(ifa);
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
        }
        sb.append(' ').append(fields.size()).append("/").append(methods.size());
        return sb.toString();
    }

    @Override
    public int compareTo(ClassSignature o) {
        int result = name.compareTo(o.name);
        if (result == 0) {
            result = Integer.compare(kind.ordinal(), o.kind.ordinal());
        }
        if (result == 0) {
            result = Integer.compare(nestingKind.ordinal(), o.nestingKind.ordinal());
        }
        if (result == 0) {
            result = supertype.compareTo(o.supertype);
        }
        if (result == 0) {
            result = MethodSignature.compareCollections(typeParams, o.typeParams);
        }
        if (result == 0) {
            result = compareCollections(ifaces, o.ifaces);
        }
        return result;
    }

    @Override
    public void hashInto(Consumer<? super Object> digest, boolean deep) {
        digest.accept(OPEN_CLASS);
        digest.accept(kind);
        digest.accept(DELIM);
        digest.accept(nestingKind);
        digest.accept(DELIM);
        digest.accept(name);
        digest.accept(DELIM);
        digest.accept(typeParams);
        digest.accept(DELIM1);
        sort(ifaces);
        digest.accept(ifaces);
        sort(fields);
        digest.accept(DELIM2);
        for (FieldSignature f : fields) {
            f.hashInto(digest, deep);
        }
        sort(methods);
        digest.accept(DELIM3);
        for (MethodSignature m : methods) {
            m.hashInto(digest, deep);
        }
        digest.accept(CLOSE_CLASS);
    }

    void enter(BiConsumer<Consumer<MethodSignature>, Consumer<FieldSignature>> c) {
        c.accept(methods::add, fields::add);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.kind);
        hash = 23 * hash + Objects.hashCode(this.name);
        hash = 23 * hash + Objects.hashCode(this.nestingKind);
        hash = 23 * hash + Objects.hashCode(this.supertype);
        hash = 23 * hash + Objects.hashCode(this.typeParams);
        hash = 23 * hash + Objects.hashCode(this.methods);
        hash = 23 * hash + Objects.hashCode(this.fields);
        hash = 23 * hash + Objects.hashCode(this.ifaces);
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
        final ClassSignature other = (ClassSignature) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.supertype, other.supertype)) {
            return false;
        }
        if (this.kind != other.kind) {
            return false;
        }
        if (this.nestingKind != other.nestingKind) {
            return false;
        }
        if (!Objects.equals(this.typeParams, other.typeParams)) {
            return false;
        }
        if (!Objects.equals(this.methods, other.methods)) {
            return false;
        }
        if (!Objects.equals(this.fields, other.fields)) {
            return false;
        }
        return Objects.equals(this.ifaces, other.ifaces);
    }
}
