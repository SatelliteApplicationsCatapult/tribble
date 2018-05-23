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
 * Return value of a fuzz test
 */
public enum FuzzResult {
    OK, // Fuzz test was not a failure and the result was not interesting
    FAILED, // The fuzz test did something non-fatal that was wrong. This should be recorded as a failure.
    INTERESTING, // The fuzz test did not fail but also did something interesting. This run should be recorded in the corpus
    IGNORE; // The fuzz test did not pass but didn't do anything interesting and should be ignored.

    public static boolean Passed(FuzzResult r) {
        return r != FAILED && r != IGNORE;
    }

}
