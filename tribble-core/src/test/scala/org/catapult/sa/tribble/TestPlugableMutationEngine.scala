package org.catapult.sa.tribble

import org.catapult.sa.tribble.CommonTest._

import org.junit.Assert._
import org.junit.Test

class TestPlugableMutationEngine {

  @Test
  def basic() : Unit = {
    val r = rand((15, 2))

    val engine = new PlugableMutationEngine(r)
    val result = engine.mutate(a(0x01, 0x02))

    assertArrayEquals(a(0x01, 0x02, 0x01, 0x02), result)
  }

  @Test
  def nullInput() : Unit = {
    val engine = new PlugableMutationEngine(rand())
    val result = engine.mutate(null)

    assertArrayEquals(a(0x00), result)
  }

  @Test
  def emptyInput() : Unit = {
    val engine = new PlugableMutationEngine(rand())
    val result = engine.mutate(a())

    assertArrayEquals(a(0x00), result)
  }
}
