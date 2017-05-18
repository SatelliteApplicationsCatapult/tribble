package org.catapult.sa.testcase

/**
  * Part of test environment. Don't ask
  */
object Fish {

  def wibble(data : Array[Byte]): Unit = {
    println("Hello world")
    if (data != null && data.length > 20 ) {
      if (data(19) >= 75) { // should be pretty rare.
        throw new Exception("BANG")
      }
      val i = data(15) | data(16) << 8 | data(17) << 16 | data(18) << 24
      val a = Array.fill(i) ("Hello World")
      println("length : " + a.map(_.length).sum)
    }
  }

}
