package org.catapult.sa

import org.catapult.sa.tribble.FuzzTest

/**
  * Created by Wil.Selwood on 13/04/2017.
  */
class TestCase extends FuzzTest {
  def test(data : Array[Byte]): Boolean = {
    println("Hello World!")
    Fish.wibble()
    if (data(0) == 0x00) {
      println("bob")
      return true
    }
    false
  }
}

