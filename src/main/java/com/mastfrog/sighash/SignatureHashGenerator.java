/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.sighash;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticListener;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 *
 * @author Tim Boudreau
 */
final class SignatureHashGenerator {

    private final Iterable<? extends Path> classpathRoots;
    private final Iterable<? extends File> outdir = Collections.singleton(
            new File(System.getProperty("java.io.tmpdir")));
    private final DiagnosticListener diagnostics = new Diagnostics();
    private final String encoding = "UTF-8";
    private Consumer<Diagnostic> onError = System.out::println;
    private final String cp;

    SignatureHashGenerator(Iterable<Path> classpathRoots) throws Exception {
        this(null, classpathRoots);
    }

    SignatureHashGenerator(String cp, Iterable<Path> classpathRoots) throws Exception {
        this.cp = cp;
        this.classpathRoots = classpathRoots;
    }

    SignatureHashGenerator(Path... classpathRoot) throws Exception {
        this((String) null, classpathRoot);
    }

    SignatureHashGenerator(String cp, Path... classpathRoot) throws Exception {
        this.cp = cp;
        this.classpathRoots = Arrays.asList(classpathRoot);
    }

    public SignatureHashGenerator onError(Consumer<Diagnostic> onError) {
        this.onError = onError;
        return this;
    }

    private void onError(Diagnostic diag) {
        onError.accept(diag);
    }

    private List<String> options() {
        // Borrowed from NetBeans
        List<String> options = new ArrayList<>(9);
        options.add("-XDide");   // Javac runs inside the IDE
        options.add("-XDsave-parameter-names");   // Javac runs inside the IDE
        options.add("-XDsuppressAbortOnBadClassFile");   // When a class file cannot be read, produce an error type instead of failing with an exception
        options.add("-XDshouldStopPolicy=GENERATE");   // Parsing should not stop in phase where an error is found
//        options.add("-attrparseonly");
        options.add("-g:source"); // Make the compiler maintian source file info
        options.add("-g:lines"); // Make the compiler maintain line table
        options.add("-g:vars");  // Make the compiler maintain local variables table
        options.add("-XDbreakDocCommentParsingOnError=false");  // Turn off compile fails for javadoc
        options.add("-proc:none"); // Do not try to run annotation processors
        if (cp != null) {
            options.add("-cp");
            options.add(cp);
        }
        return options;
    }

    void go(HashBuilder receiver) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(),
                Charset.forName(encoding));
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, outdir);
        Iterable<? extends JavaFileObject> toCompile = findSources(fileManager);

        CompilationTask task = compiler.getTask(null,
                fileManager, diagnostics, options(), null, toCompile);

        parse((JavacTask) task, receiver);
    }

    private void parse(JavacTask task, HashBuilder receiver) throws Exception {
        Trees trees = Trees.instance(task);
        List<Element> els = new ArrayList<>();
        for (Element el : task.analyze()) {
            switch (el.getKind()) {
                case CLASS:
                case ENUM:
                case INTERFACE:
                    els.add(el);
                    break;
            }
        }
        for (Element el : els) {
            handleOneClass(task, trees, (TypeElement) el, receiver);
        }
    }

    private void handleOneClass(JavacTask task, Trees trees, TypeElement type, HashBuilder receiver) throws Exception {
        receiver.enterClass(task, type.asType(), type, trees);
    }

    private Iterable<JavaFileObject> findSources(final StandardJavaFileManager mgr) throws IOException {
        Set<JavaFileObject> paths = new HashSet<>();
        for (Path classpathRoot : classpathRoots) {
            Files.walkFileTree(classpathRoot, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().endsWith(".java")) {
                        for (JavaFileObject fo : mgr.getJavaFileObjects(file.toFile())) {
                            paths.add(fo);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return paths;
    }

    final class Diagnostics implements DiagnosticListener {

        @Override
        public void report(Diagnostic diagnostic) {
            onError(diagnostic);
        }
    }
}
