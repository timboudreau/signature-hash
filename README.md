Signature Hash
==============

A library / utility which can compute a consistent, repeatable hash of the signatures of Java methods in one or more source directories.

A signature hash incorporates source code elements for *public* and *protected* classes and class-members, while ignoring things such
as formatting, annotations and other source elements that do not directly affect binary compatibility.

It operates in one of two modes:

 * Shallow mode - only hash signatures of public classes, methods, constructors and fields
 * Deep mode - drill through the source code in visible constructors and methods, hashing those elements that matter to control flow, while avoiding hashing things which can be changed 
without affecting compatibility.  This is more useful via the API, with individual member hashes, to answer the question _Did anything **my code calls** change **at all** since the
previous release?_ - from the top level, deep mode gives you a hash that may be stable across insignificant code changes (reformatting, docs, annotations) but is likely to differ
if call sequence changes.

Usage
-----

As a standalone tool, simply pass the classpath and source folder, e.g.

```sh
java -jar sighash.jar [--deep] -cp path/to/jar1.jar:path/to/jar2.jar source/path generated-sources/path
```

The default algorithm used is SHA-512.


Implementation
--------------

Internally, it runs javac and uses the javac tree api to build a tree of all classes, their fields, constructors and methods on the source path, converting relevant
parameters to strings or other appropriate objects.  These are then sorted so order is consistent across runs against the same sources, and added to the
MessageDigest that creates the hash, which is then output as a URL-safe base-64 string.

Some not yet implemented optimizations and features are possible:

 * In deep mode, it currently appends to what can become a very large StringBuilder (since the hashable 
elements of the closure of all invoked method bodies with source are also appended).
This is good for debugging purposes, but updating the digest and discarding elements would be much faster.
 * Generate a flat file with the hashes of each public class and member, so that basic signature diffs, and 
answering useful questions such as whether any code paths your code touches were altered in a new library revision is simple
