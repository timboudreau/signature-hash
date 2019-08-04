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
