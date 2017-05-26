package org.catapult.sa.tribble

import java.nio.charset.StandardCharsets

import org.junit.Assert._
import org.junit.Test


class TestGenerator {

  @Test
  def testFixedValue(): Unit = {
    val r = new FixedValue("hello world".getBytes(StandardCharsets.UTF_8))
    assertEquals(11, r.length())

    val resultArray = new Array[Byte](11)
    r.value(resultArray, 0)
    assertEquals("hello world", new String(resultArray, StandardCharsets.UTF_8))
  }

}
