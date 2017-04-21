package org.catapult.sa

/**
  * Part of test environment. Don't ask
  */
object Fish {

  def wibble(data : Array[Byte]): Unit = {
    println("Hello world")
    if (data != null && data.length > 20 ) {
      if (data(19) >= 65) { // should be pretty rare.
        throw new Exception("BANG")
      }
    }
  }

}
