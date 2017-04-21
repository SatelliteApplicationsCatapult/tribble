package org.catapult.sa.testcase

import org.catapult.sa.tribble.FuzzTest

/**
  * Really simple stupid test case
  */
class TestCase extends FuzzTest {
  def test(data : Array[Byte]): Boolean = {
    println("Hello World!")
    Fish.wibble(data)
    if (data(0) == 0x00) {
      println("bob")
      return true
    }
    true
  }
}

