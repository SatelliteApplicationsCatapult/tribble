package org.catapult.sa.tribble;

/**
 * Tag for method that implements a fuzz test.
 */
public interface FuzzTest {
    /**
     * Defines a fuzz test. The input data for the test will be passed in
     * @param data input bytes for this test.
     * @return return true if this is a successful test result or false if not.
     */
    boolean test(byte[] data);
}
