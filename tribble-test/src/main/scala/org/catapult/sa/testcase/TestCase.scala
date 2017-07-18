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

