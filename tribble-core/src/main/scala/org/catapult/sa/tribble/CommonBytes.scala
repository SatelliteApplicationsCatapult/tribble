package org.catapult.sa.tribble

/**
  * Utility functions for dealing with bytes
  */
object CommonBytes {
  /**
    * Short hand converstion of int to a byte
    * @param i input int to convert
    * @return byte value created from the int.
    */
  def b(i: Int): Byte = i.asInstanceOf[Byte]

  /**
    * Short hand construction of an array of bytes from a set of ints.
    * @param a incoming ints.
    * @return the incoming ints mapped to bytes and made an array.
    */
  def a(a : Int *) : Array[Byte] = a.map(b).toArray
}
