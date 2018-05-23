/*
 *    Copyright 2018 Satellite Applications Catapult Limited.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
