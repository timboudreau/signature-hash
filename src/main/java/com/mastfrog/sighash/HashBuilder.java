package com.mastfrog.sighash;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author Tim Boudreau
 */
interface HashBuilder {

    void enterClass(JavacTask task, TypeMirror type, TypeElement element, Trees trees);

}
