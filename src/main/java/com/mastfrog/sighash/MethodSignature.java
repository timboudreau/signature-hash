package com.mastfrog.sighash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author Tim Boudreau
 */
public final class MethodSignature implements Signature, Comparable<MethodSignature> {

    private final String name;
    private final String returnType;
    private List<String> parameterTypes;
    private Set<String> thrownTypes;
    private final Set<Modifier> modifiers;
    private List<String> typeParamBounds;
    // These values are somewhat random, and simply used to clearly
    // delimit items where two adjacent items and a single item that
    // concatenates the two items would otherwise have the same hash.
    // Uses values that are outside the typical ascii range to improve
    // the hash.
    static final byte[] OPEN_METHOD = bytes(0xE4, 0xC3, 0x3);
    static final byte[] CLOSE_METHOD = bytes(0xE3, 0xC2, 0x4, 0x06, 0xE0);
    static final byte[] DELIM_1 = bytes(0xE2, 0xC1, 0x05);
    static final byte[] DELIM_2 = bytes(0xC1, 0xE1, 0x06, 0x31, 0xFF);
    static final byte[] DELIM_3 = bytes(0xF0, 0x09, 0xCF);
    static final byte[] DELIM_4 = bytes(0xF9, 0xB9, 0xF1);

    private List<Signature> drilldown;

    MethodSignature(ExecutableElement el) {
        this.name = el.getSimpleName().toString();
        this.returnType = el.getReturnType().toString();
        modifiers = el.getModifiers();
        List<? extends VariableElement> params = el.getParameters();
        if (!params.isEmpty()) {
            parameterTypes = new ArrayList<>();
            for (VariableElement p : el.getParameters()) {
                parameterTypes.add(typeToString(p.asType()));
            }
        }
        List<? extends TypeMirror> thrown = el.getThrownTypes();
        if (!thrown.isEmpty()) {
            thrownTypes = new TreeSet<>();
            for (TypeMirror tm : thrown) {
                thrownTypes.add(typeToString(tm));
            }
        }
        List<? extends TypeParameterElement> typeParams = el.getTypeParameters();
        if (!typeParams.isEmpty()) {
            for (TypeParameterElement typeParam : typeParams) {
                List<? extends TypeMirror> bounds = typeParam.getBounds();
                if (!bounds.isEmpty()) {
                    if (typeParamBounds == null) {
                        typeParamBounds = new ArrayList<>();
                    }
                    StringBuilder sb = new StringBuilder(64).append(':');
                    for (TypeMirror bound : bounds) {
                        sb.append(typeToString(bound));
                    }
                    typeParamBounds.add(sb.toString());
                }
            }
        }
    }

    void enter(Consumer<Consumer<Signature>> c) {
        if (drilldown == null) {
            drilldown = new ArrayList<>();
        }
        c.accept(drilldown::add);
    }

    static <T extends Comparable<T>> void sort(List<T> list) {
        if (list != null) {
            Collections.sort(list);
        }
    }

    static <T extends Enum<T>> int compareEnumSets(Collection<? extends T> a, Collection<? extends T> b) {
        if (a == null && b != null) {
            return 1;
        } else if (a != null && b == null) {
            return -1;
        } else if (a == null && b == null) {
            return 0;
        }
        int result = Integer.compare(a.size(), b.size());
        if (result != 0) {
            return result;
        }
        List<String> al = new ArrayList<>(a.size());
        List<String> bl = new ArrayList<>(b.size());
        for (T aa : a) {
            al.add(aa.name());
        }
        for (T bb : b) {
            bl.add(bb.name());
        }
        Collections.sort(al);
        Collections.sort(bl);
        return compareCollections(al, bl);
    }

    static byte[] bytes(int... ints) {
        byte[] b = new byte[ints.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) ints[i];
        }
        return b;
    }

    @Override
    public int compareTo(MethodSignature o) {
        int result = name.compareTo(o.name);
        if (result == 0) {
            result = returnType.compareTo(o.returnType);
        }
        if (result == 0) {
            result = compareCollections(parameterTypes, o.parameterTypes);
        }
        if (result == 0) {
            result = compareCollections(thrownTypes, o.thrownTypes);
        }
        if (result == 0) {
            result = compareCollections(typeParamBounds, o.typeParamBounds);
        }
        if (result == 0) {
            result = compareEnumSets(modifiers, o.modifiers);
        }
        return result;
    }

    static int compareCollections(Collection<String> a, Collection<String> b) {
        if (a == null && b != null) {
            return 1;
        } else if (a != null && b == null) {
            return -1;
        } else if (a == null && b == null) {
            return 0;
        } else {
            int result = Integer.compare(a.size(), b.size());
            if (result == 0) {
                Iterator<String> ai = a.iterator();
                Iterator<String> bi = b.iterator();
                while (ai.hasNext()) {
                    String aCurr = ai.next();
                    String bCurr = bi.next();
                    result = aCurr.compareTo(bCurr);
                    if (result != 0) {
                        break;
                    }
                }
            }
            return result;
        }
    }

    @Override
    public void hashInto(Consumer<? super Object> digest, boolean deep) {
        digest.accept(OPEN_METHOD);
        for (Modifier m : modifiers) {
            boolean visible = false;
            boolean isFinal = false;
            switch (m) {
                case PROTECTED:
                case PUBLIC:
                    visible = true; // fallthrough
                case PRIVATE:
                    digest.accept(m);
                    break;
                case FINAL:
                    isFinal = true;
                    break;
                default:
                // do nothing - native, etc. irrelevant
            }
            if (isFinal && visible) {
                digest.accept(Modifier.FINAL);
            }
        }
        digest.accept(DELIM_1);
        digest.accept(name);
        digest.accept(DELIM_1);
        digest.accept(returnType);
        digest.accept(DELIM_2);
        if (typeParamBounds != null) {
            digest.accept(typeParamBounds);
        }
        digest.accept(DELIM_3);
        if (parameterTypes != null) {
            digest.accept(parameterTypes);
        }
        digest.accept(DELIM_4);
        if (thrownTypes != null) {
            digest.accept(thrownTypes);
        }
        if (deep) {
            digest.accept("{");
            if (drilldown != null) {
                for (Signature dd : drilldown) {
                    dd.hashInto(digest, deep);
                }
            }
            digest.accept("}");
        }
        digest.accept(CLOSE_METHOD);
    }

    static String typeToString(TypeMirror mir) {
        // XXX drill through with a type visitor to omit spurious differences?
        return mir.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.name);
        hash = 47 * hash + Objects.hashCode(this.returnType);
        hash = 47 * hash + Objects.hashCode(this.parameterTypes);
        hash = 47 * hash + Objects.hashCode(this.thrownTypes);
        hash = 47 * hash + Objects.hashCode(this.modifiers);
        hash = 47 * hash + Objects.hashCode(this.typeParamBounds);
        hash = 47 * hash + Objects.hashCode(this.drilldown);
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
        final MethodSignature other = (MethodSignature) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.returnType, other.returnType)) {
            return false;
        }
        if (!Objects.equals(this.parameterTypes, other.parameterTypes)) {
            return false;
        }
        if (!Objects.equals(this.thrownTypes, other.thrownTypes)) {
            return false;
        }
        if (!Objects.equals(this.modifiers, other.modifiers)) {
            return false;
        }
        if (!Objects.equals(this.typeParamBounds, other.typeParamBounds)) {
            return false;
        }
        return Objects.equals(this.drilldown, other.drilldown);
    }
}
