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

package org.catapult.sa.tribble.stats

import org.junit.Assert._
import org.junit.Test

/**
  * Quick test of the duration formatting in stats.
  */
class TestStats {

  val tests = List(
    (0L, "0S"),
    (45000L, "45S"),
    (4500L, "4S 500ms"),
    (5L, "5ms"),
    (67000L, "1M 7S"),
    (70230L, "1M 10S 230ms"),
    (3606000L, "1H 6S")
  )

  @Test
  def runTests() : Unit = {
    tests.zipWithIndex.foreach { t =>
      val result = CurrentStats.formatDuration(t._1._1)

      assertEquals(s"Wrong result for entry ${t._2}, input ${t._1._1}", t._1._2, result)
    }
  }

}
