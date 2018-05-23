package org.catapult.sa.tribble.stats

import org.junit.Assert._
import org.junit.Test

/**
  * Quick test of the duration formatting in stats.
  */
class TestStats {

  val tests = List(
    (0L, "0S"),
    (45000L, "45S"),
    (4500L, "4S 500ms"),
    (5L, "5ms"),
    (67000L, "1M 7S"),
    (70230L, "1M 10S 230ms"),
    (3606000L, "1H 6S")
  )

  @Test
  def runTests() : Unit = {
    tests.zipWithIndex.foreach { t =>
      val result = CurrentStats.formatDuration(t._1._1)

      assertEquals(s"Wrong result for entry ${t._2}, input ${t._1._1}", t._1._2, result)
    }
  }

}
