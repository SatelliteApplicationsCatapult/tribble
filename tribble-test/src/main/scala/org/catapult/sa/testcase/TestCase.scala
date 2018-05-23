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

package org.catapult.sa.testcase

import org.catapult.sa.tribble.{FuzzResult, FuzzTest}

/**
  * Really simple stupid test case
  */
class TestCase extends FuzzTest {

  def test(data : Array[Byte]): FuzzResult = {
    println("Hello World!")
    Fish.wibble(data)

    if (!data.isEmpty && data(0) == 0x00) {
      println("bob")
      return FuzzResult.OK
    }
    return FuzzResult.OK
  }
}

