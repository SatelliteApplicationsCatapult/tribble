package org.catapult.sa.tribble;

/**
 * Tag for method that implements a fuzz test.
 *
 * Note: The implementing class must have a default constructor.
 */
public interface FuzzTest {
    /**
     * Defines a fuzz test. The input data for the test will be passed in
     * @param data input bytes for this test.
     * @return return true if this is a successful test result or false if not.
     * @throws Exception if there is any problem with the test case. Any exceptions thrown out of the test case are
     * considered to be failures.
     */
    boolean test(byte[] data) throws Exception;
}
