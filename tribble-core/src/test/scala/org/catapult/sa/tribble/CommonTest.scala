package org.catapult.sa.tribble

import org.mockito.Mockito.{mock, when}

import scala.util.Random

/**
  * Simple utility methods to make tests easier.
  */
object CommonTest {
  def a(a : Int *) : Array[Byte] = a.map(_.asInstanceOf[Byte]).toArray

  def rand(r : (Int, Int) *) : Random = {
    val result = mock(classOf[Random])
    r.groupBy(_._1).foreach{ i =>
      i._2.foldLeft(when(result.nextInt(i._1))) {(w, l) => w.thenReturn(l._2)}
    }
    result
  }
}
