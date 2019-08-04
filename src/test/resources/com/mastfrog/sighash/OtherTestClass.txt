package com.mastfrog.sighash;

public class OtherTestClass {

    public int add(int a, int b) {
        return recurse(a, b);
    }

    public int recurse(int val, int times) {
        int nue = val + times;
        if (times > 1) {
            return recurse(nue, times - 1);
        }
        return nue;
    }

    public int mutualOne(int ix) {
        if (ix == 0) {
            return ix;
        }
        return mutualTwo(ix);
    }

    public int mutualTwo(int ix) {
        if (ix == 0) {
            return ix;
        }
        return mutualOne(ix);
    }
}
