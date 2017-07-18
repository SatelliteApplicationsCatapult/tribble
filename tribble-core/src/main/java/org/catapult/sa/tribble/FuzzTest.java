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
     * @return return FuzzResult describing this result.
     * @throws Exception if there is any problem with the test case. Any exceptions thrown out of the test case are
     * considered to be failures.
     */
    FuzzResult test(byte[] data) throws Exception;
}
