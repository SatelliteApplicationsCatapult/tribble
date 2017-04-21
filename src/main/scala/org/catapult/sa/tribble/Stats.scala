package org.catapult.sa.tribble

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

/**
  * Keep track of the stats. Number of runs, number of crashes etc
  *
  * Later this should be used for displaying a running log as the program is running.
  */
class Stats {

  private val runs  = new AtomicLong(0)
  private val fails = new AtomicLong(0)
  private val paths = new AtomicInteger(0)

  def addRun(success : Boolean, newPath : Boolean): Unit = {
    runs.incrementAndGet()
    if (!success) {
      fails.incrementAndGet()
    }

    if (newPath) {
      paths.incrementAndGet()
    }
  }

  def getStats() : CurrentStats = {
    CurrentStats(runs.get(), fails.get(), paths.get())
  }

}

case class CurrentStats(runs : Long, fails : Long, paths : Int) {
  override def toString: String = s"runs: $runs fails: $fails paths $paths"
}