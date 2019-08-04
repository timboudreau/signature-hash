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

import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Main entry point.  Typical use is
 * <code>SigTree.create(classpath, srcDir, otherSrcDir).hash("SHA-512",false)</code>.
 * <p>
 * Runs javac, and builds a tree of public and protected method signatures.
 * In "deep" mode, methods and constructors signatures will also incorporate a
 * drill-down through the code they contain, avoiding variable names but incorporating
 * the sequence of javac tree elements that occur within it, <i>and the closure
 * of any methods called which javac can find the source code to</i>.  Deep
 * mode is more useful when splitting out hashes by method, and can be used
 * to answer the question "has any code path that I call been changed since
 * the previous hash".
 * </p>
 * @author Tim Boudreau
 */
public final class SigTree implements Signature, Iterable<ClassSignature> {

    private final Set<ClassSignature> children = new TreeSet<>();

    private SigTree() {

    }

    public Iterator<ClassSignature> iterator() {
        return Collections.unmodifiableCollection(children).iterator();
    }

    public static void main(String[] args) throws Exception {
        boolean deep = false;
        List<Path> paths = new ArrayList<>();
        boolean nextIsClasspath = false;
        String classpath = null;
        for (String arg : args) {
            if ("--deep".equals(arg) || "-d".equals(arg)) {
                deep = true;
            } else if ("-cp".equals(arg) || "--class-path".equals(arg)) {
                nextIsClasspath = true;
                continue;
            } else if (nextIsClasspath) {
                classpath = arg;
            } else {
                Path path = Paths.get(arg);
                if (!Files.exists(path)) {
                    System.err.println("Does not exist: " + path);
                    System.exit(1);
                }
                if (!Files.isDirectory(path)) {
                    System.err.println("Not a directory: " + path);
                    System.exit(1);
                }
                paths.add(path);
            }
            nextIsClasspath = false;
        }
        if (paths.isEmpty()) {
            System.err.println("No files specified.");
            System.err.println("Usage: java -jar sighash.jar [--deep] "
                    + "-cp /class/path/a:/class/path/b source/dir/a source/dir/b");
            System.exit(2);
        }
        SigTree tree = SigTree.create(classpath, paths.toArray(new Path[0]));
        Hasher hash = new Hasher(MessageDigest.getInstance("SHA-512"));
        tree.hashInto(hash, deep);
        System.out.println(hash.toString());
    }

    private static final class PublicProtectedPredicate implements Predicate<Element> {

        @Override
        public boolean test(Element t) {
            Set<Modifier> modifiers = t.getModifiers();
            return modifiers != null && modifiers.contains(PUBLIC) || modifiers.contains(PROTECTED);
        }
    }

    public static SigTree create(String cp, Path... paths) throws Exception {
        PublicProtectedPredicate pred = new PublicProtectedPredicate();
        SignatureHashGenerator gen = new SignatureHashGenerator(cp, paths);
        SigTree tree = new SigTree();
        gen.go((JavacTask task, TypeMirror type, TypeElement element, Trees trees) -> {
            tree.add(task, element, pred, trees);
        });
        return tree;
    }

    public static SigTree create(Path... paths) throws Exception {
        return create(null, paths);
    }

    public String hash(String algorithm, boolean deep) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        Hasher hasher = new Hasher(digest);
        hashInto(hasher, deep);
        return hasher.toString();
    }

    @Override
    public void hashInto(Consumer<? super Object> digest, boolean deep) {
        for (Signature tree : children) {
            tree.hashInto(digest, deep);
        }
    }

    public void add(JavacTask task, TypeElement type, Predicate<? super Element> include, Trees trees) {
        if (include.test(type)) {
            ClassSignature clazz = new ClassSignature(type);
            children.add(clazz);
            clazz.enter((methods, fields) -> {
                for (Element child : type.getEnclosedElements()) {
                    if (!include.test(child)) {
                        continue;
                    }
                    switch (child.getKind()) {
                        case METHOD:
                        case CONSTRUCTOR:
                            MethodSignature msig = new MethodSignature((ExecutableElement) child);
                            drilldown(task, msig, trees, ((ExecutableElement) child));
                            methods.accept(msig);
                            break;
                        case ENUM_CONSTANT:
                        case FIELD:
                            fields.accept(new FieldSignature((VariableElement) child));
                            break;
                    }
                }
            });
        }
    }

    private void drilldown(JavacTask task, MethodSignature msig, Trees trees, ExecutableElement method) {
        TreePath pth = trees.getPath(method);
        if (pth == null) {
            // This will happpen with, for example, Enum classes' values() methods
            return;
        }
        msig.enter(sigConsumer -> {
            assert pth.getCompilationUnit() != null : "Comp unit is null";
            TV tv = new TV(task);
            StringBuilder sig = new StringBuilder();
            tv.scan(pth, sig);
            sigConsumer.accept(new CodeSig(sig));
        });
    }

    static TypeElement enclosingType(Element el) {
        while (el != null && !(el instanceof TypeElement)) {
            el = el.getEnclosingElement();
        }
        return el instanceof TypeElement ? ((TypeElement) el) : null;
    }

    private static final class CodeSig implements Signature {

        private final StringBuilder sb;

        public CodeSig(StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        public void hashInto(Consumer<? super Object> digest, boolean deep) {
            if (!deep) {
                return;
            }
            digest.accept(sb);
        }

    }

    // Scanner which is used to drill through source code
    private static final class TV extends TreePathScanner<Void, StringBuilder> {

        private final JavacTask task;
        private final Map<String, String> subs;

        // XXX using a stringbuilder here is much more debuggable, but can
        // be expensive in memory since it concatenates the closure of anything
        // called that the source can be found to.  It would be straightforward
        // to just add it to the hasher / messagedigest as we go.

        TV(JavacTask task, Map<String, String> subs) {
            this.subs = subs;
            this.task = task;
        }

        TV(JavacTask task) {
            this.task = task;
            this.subs = new HashMap<>();
        }

        @Override
        public Void scan(TreePath path, StringBuilder sb) {
            return super.scan(path, sb);
        }

        @Override
        public Void scan(Tree tree, StringBuilder p) {
            if (tree != null) {
                switch (tree.getKind()) {
                    case MODIFIERS:
                    case ANNOTATION:
                    case ANNOTATED_TYPE:
                    case BLOCK:
                    case EMPTY_STATEMENT:
                    case COMPILATION_UNIT:
                    case IMPORT:
                    case PACKAGE:
                    case USES:
                    case REQUIRES:
                    case LABELED_STATEMENT:
                    case OPENS:
                    case STRING_LITERAL:
                    case SUPER_WILDCARD:
                    case EXTENDS_WILDCARD:
                    case EXPORTS:
                    case TYPE_ANNOTATION:
                    case INTERSECTION_TYPE:
                    case PROVIDES:
                        break;
                    default:
                        p.append(tree.getKind().name()).append(' ');

                }
            }
            return super.scan(tree, p);
        }

        @Override
        public Void visitReturn(ReturnTree node, StringBuilder p) {
            return super.visitReturn(node, p);
        }

        @Override
        public Void visitSynchronized(SynchronizedTree node, StringBuilder p) {
            return super.visitSynchronized(node, p);
        }

        private boolean collecting;

        Void collectIds(Runnable r) {
            boolean old = collecting;
            collecting = true;
            r.run();
            collecting = old;
            return null;
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, StringBuilder p) {
            if (collecting) {
                p.append(node.toString());
            }
            return super.visitIdentifier(node, p);
        }

        @Override
        public Void visitMemberReference(MemberReferenceTree node, StringBuilder p) {
            return collectIds(() -> {
                p.append(node.getMode()).append(' ');
                super.visitMemberReference(node, p);
            });
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, StringBuilder p) {
            return collectIds(() -> {
                super.visitMemberSelect(node, p);
            });
        }

        @Override
        public Void visitInstanceOf(InstanceOfTree node, StringBuilder p) {
            p.append(node.getType()).append(' ');
            return super.visitInstanceOf(node, p);
        }

        @Override
        public Void visitNewArray(NewArrayTree node, StringBuilder p) {
            p.append(node.getType()).append(' ');
            return super.visitNewArray(node, p);
        }

        @Override
        public Void visitNewClass(NewClassTree node, StringBuilder p) {
            p.append(node.getIdentifier()).append(' ');
            return super.visitNewClass(node, p);
        }

        private String scanCurrentPathAsElement() {
            return scanAsElement(getCurrentPath());
        }

        private String scanAsElement(TreePath path) {
            Trees trees = Trees.instance(task);
            Element el = trees.getElement(path);
            if (el == null) {
                // Throws does not convert to an element
                return path.getLeaf().toString();
            }
            TypeElement type = enclosingType(el);

            String key = type.getQualifiedName().toString() + '.' + el.getSimpleName();
            String result = subs.get(key);
            if (result != null) {
                return result;
            }
            Tree treeForEl = trees.getTree(el);
            if (treeForEl == null) {
                subs.put(key, key);
                return key;
            }
            TreePath newPath = Trees.instance(task).getPath(el);
            if (newPath.equals(getCurrentPath())) {
                return "<recurse-" + key + ">";
            }
            TV tv = new TV(task, subs);
            subs.put(key, "<recurse-" + key + ">");
            StringBuilder sb = new StringBuilder();
            tv.scan(newPath, sb);
            subs.put(key, result = sb.toString());
            return result;
        }

        private static final class NameFinder extends TreeScanner<String, Void> {

            @Override
            public String reduce(String r1, String r2) {
                return r2 == null ? r1 : r2;
            }

            @Override
            public String visitIdentifier(IdentifierTree node, Void p) {
                String result = node.getName().toString();
                super.visitIdentifier(node, p);
                return result;
            }
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, StringBuilder p) {
            // append the *last* name found, e.g. "foo" for an invocation of in x.bar.foo()
            p.append(new NameFinder().scan(node.getMethodSelect(), null));
            String sub = scanCurrentPathAsElement();
            if (sub != null) {
                p.append(sub).append(' ');
            }
            return super.visitMethodInvocation(node, p);
        }

        @Override
        public Void visitThrow(ThrowTree node, StringBuilder p) {
            p.append(scanCurrentPathAsElement()).append(' ');
            return super.visitThrow(node, p);
        }

        @Override
        public Void visitLiteral(LiteralTree node, StringBuilder p) {
            p.append(node.getValue()).append(' ');
            return super.visitLiteral(node, p);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, StringBuilder p) {
            p.append(scanCurrentPathAsElement()).append(' ');
            return super.visitCompoundAssignment(node, p);
        }

        @Override
        public Void visitVariable(VariableTree node, StringBuilder p) {
            Element el = Trees.instance(task).getElement(getCurrentPath());
            p.append(el.asType()).append(' ');
            return super.visitVariable(node, p);
        }

        @Override
        public Void visitMethod(MethodTree node, StringBuilder p) {
            p.append(node.getName()).append(' ');
            return super.visitMethod(node, p);
        }
    }
}
