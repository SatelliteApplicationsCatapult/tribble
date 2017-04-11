package org.catapult.sa.tribble

import scala.util.Random

/**
  * Generator for a byte array content for a fuzzing run.
  */
class Generator {

}

object Generator {
  // TODO: Thread local
  val r = new Random()
}

trait Section {
  def length() : Int
  def value(in : Array[Byte], offset : Int)
  def Evolve(interesting : Boolean) : List[Section]
}

class FixedValue(value : Array[Byte]) extends Section {

  override def length(): Int = value.length

  override def value(in: Array[Byte], offset: Int): Unit =  {
    value.zipWithIndex.foreach { i : (Byte, Int) => {
      in.update(offset + i._2, i._1)
    }}
  }

  override def Evolve(interesting: Boolean): List[Section] = {
    if (interesting) {

    } else {
      List(new RandomValue(value.length))
    }
  }
}

class RandomValue(length : Int) extends Section {

  val value = new Array[Byte](length)
  Generator.r.nextBytes(value)

  override def length(): Int = length

  override def value(in: Array[Byte], offset: Int): Unit = {
    value.zipWithIndex.foreach { i : (Byte, Int) => {
      in.update(offset + i._2, i._1)
    }}
  }

  override def Evolve(interesting: Boolean): List[Section] = {
    if (interesting) {
      List(new FixedValue(value))
    } else {
      List(new RandomValue(length))
    }
  }
}

