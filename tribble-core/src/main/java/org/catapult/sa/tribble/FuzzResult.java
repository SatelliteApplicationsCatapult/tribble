package org.catapult.sa.tribble;

/**
 * Return value of a fuzz test
 */
public enum FuzzResult {
    OK, // Fuzz test was not a failure and the result was not interesting
    FAILED, // The fuzz test did something non-fatal that was wrong. This should be recorded as a failure.
    INTERESTING; // The fuzz test did not fail but also did something interesting. This run should be recorded in the corpus

    public static boolean Passed(FuzzResult r) {
        return r != FAILED;
    }

}
