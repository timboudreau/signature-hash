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
