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

import com.mastfrog.util.file.FileUtils;
import com.mastfrog.util.streams.Streams;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class SignatureHashGeneratorTest {

    Path root;
    Path root2;

    @Test
    public void testSomeMethod() throws Exception {
        SigTree tree = SigTree.create(root);
        Hasher hasher = new Hasher(MessageDigest.getInstance("SHA-512"));
        tree.topLevelHash(hasher);
        System.out.println("HASH: " + hasher);
        System.out.println("\n\n");

        Hasher hash2 = new Hasher(MessageDigest.getInstance("SHA-512"));
        tree.deepHash(hash2);
        System.out.println("HASH DEEP: " + hash2);

        SigTree tree2 = SigTree.create(root2);
        Hasher hash3 = new Hasher(MessageDigest.getInstance("SHA-512"));
        tree2.topLevelHash(hash3);
        System.out.println("HASH2: " + hash3);

        Hasher hash4 = new Hasher(MessageDigest.getInstance("SHA-512"));
        tree2.deepHash(hash4);
        System.out.println("HASH2 DEEP: " + hash4);

        if (!hasher.toString().equals(hash3.toString())) {
            fail(compare("shallow hash mismatch", tree, tree2, false));
        }

        assertEquals(hasher.toString(), hash3.toString());

        if (!hash2.toString().equals(hash4.toString())) {
            fail(compare("shallow hash mismatch", tree, tree2, true));
        }
        assertEquals(hash2.toString(), hash4.toString());

        for (ClassSignature c : tree) {
            System.out.println(" - " + c);
            for (FieldSignature f : c.fields()) {
                System.out.println("   - " + f);
            }
            for (MethodSignature m : c.methods()) {
                System.out.println("   - " + m);
            }
        }
    }

    private String compare(String msg, SigTree a, SigTree b, boolean deep) {
        StringBuilder sb = new StringBuilder(msg);
        List<Object> as = new ArrayList<>();
        List<Object> bs = new ArrayList<>();
        a.hashInto(new C(as), deep);
        b.hashInto(new C(bs), deep);
        if (as.equals(bs)) {
            sb.append(" - lists match but hashes do not");
        } else {
            if (as.size() != bs.size()) {
                sb.append(" - sizes do not match - ").append(as.size()).append(" and ").append(bs.size());
            }
            int max = Math.min(as.size(), bs.size());
            int mismatchAt = -1;
            for (int i = 0; i < max; i++) {
                Object aa = as.get(i);
                Object bb = bs.get(i);
                if (!Objects.equals(aa, bb)) {
                    mismatchAt = i;
                    break;
                }
            }
            if (mismatchAt >= 0) {
                int start = Math.max(0, mismatchAt - 3);
                sb.append('\n');
                max = Math.min(start + 6, max);
                for (int i = start; i < max; i++) {
                    Object aa = as.get(i);
                    Object bb = bs.get(i);
                    if (Objects.equals(aa, bb)) {
                        sb.append("  ").append(i).append(". ");
                    } else {
                        sb.append("X ").append(i).append(". ");
                    }
                    sb.append(aa).append(" / ").append(bb);
                }
            }
        }
        return sb.toString();
    }

    private static final class C implements Consumer<Object> {

        private final List<Object> objs;

        public C(List<Object> objs) {
            this.objs = objs;
        }

        @Override
        public void accept(Object t) {
            if (t instanceof byte[]) {
                objs.add(Arrays.toString((byte[]) t));
            } else {
                objs.add(t);
            }
        }

    }

    @BeforeEach
    public void before() throws IOException {
        root = FileUtils.newTempDir();
        root2 = FileUtils.newTempDir();

        Path cp = root.resolve("com/mastfrog/sighash");
        Path cp2 = root2.resolve("com/mastfrog/sighash");
        Files.createDirectories(cp);
        Files.createDirectories(cp2);
        Path sourceCode = cp.resolve("TestClass.java");
        Path sourceCode2 = cp2.resolve("TestClass.java");
        Files.createFile(sourceCode);
        Files.createFile(sourceCode2);
        String testClassContent = Streams.readResourceAsUTF8(SignatureHashGeneratorTest.class, "TestClass.txt");
        FileUtils.writeUtf8(sourceCode, testClassContent);
        FileUtils.writeUtf8(sourceCode2, testClassContent.replaceAll("\\s+", " "));

        Path moreSourceCode = cp.resolve("OtherTestClass.java");
        Path moreSourceCode2 = cp2.resolve("OtherTestClass.java");
        Files.createFile(moreSourceCode);
        Files.createFile(moreSourceCode2);
        String moreTestContent = Streams.readResourceAsUTF8(SignatureHashGeneratorTest.class, "OtherTestClass.txt");
        FileUtils.writeUtf8(moreSourceCode, moreTestContent);
        FileUtils.writeUtf8(moreSourceCode2, moreTestContent.replaceAll("\\s+", " "));
    }

    @AfterEach
    public void after() throws IOException {
        FileUtils.deltree(root);
        FileUtils.deltree(root2);
    }
}
