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

package org.catapult.sa.tribble

import org.catapult.sa.tribble.CommonBytes._
import org.catapult.sa.tribble.CommonTest._

import org.junit.Assert._
import org.junit.Test

class TestPlugableMutationEngine {

  @Test
  def basic() : Unit = {
    val r = rand((17, 2))

    val engine = new PlugableMutationEngine(r)
    val result = engine.mutate(a(0x01, 0x02))

    assertArrayEquals(a(0x01, 0x02, 0x01, 0x02), result._1)
  }

  @Test
  def nullInput() : Unit = {
    val engine = new PlugableMutationEngine(rand())
    val result = engine.mutate(null)

    assertArrayEquals(a(0x00), result._1)
  }

  @Test
  def emptyInput() : Unit = {
    val engine = new PlugableMutationEngine(rand())
    val result = engine.mutate(a())

    assertArrayEquals(a(0x00), result._1)
  }
}
