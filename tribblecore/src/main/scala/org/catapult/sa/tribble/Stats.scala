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
  private val totalTime = new AtomicLong(0)
  private val minTime = new AtomicLong(Long.MaxValue)
  private val maxTime = new AtomicLong(Long.MinValue)

  def addRun(success : Boolean, newPath : Boolean, time : Long): Unit = {
    runs.incrementAndGet()
    if (!success) {
      fails.incrementAndGet()
    }

    if (newPath) {
      paths.incrementAndGet()
    }

    totalTime.addAndGet(time)

    var setSuccess = false
    do {
      val minT = minTime.get()
      if (minT > time) {
        setSuccess = minTime.compareAndSet(minT, time)
      } else {
        setSuccess = true
      }
    } while (!setSuccess)

    setSuccess = false
    do {
      val maxT = maxTime.get()
      if (maxT < time) {
        setSuccess = maxTime.compareAndSet(maxT, time)
      } else {
        setSuccess = true
      }
    } while (!setSuccess)

  }

  def getStats : CurrentStats = {
    val tt = totalTime.get()
    val r = runs.get()
    val avg = if (r == 0) 0 else tt/r
    CurrentStats(r, fails.get(), paths.get(), tt, avg, minTime.get(), maxTime.get())
  }

}

case class CurrentStats(runs : Long, fails : Long, paths : Int, totalTime : Long, averageTime : Long, minTime : Long, maxTime : Long) {
  override def toString: String = s"runs: $runs fails: $fails paths: $paths total time: $totalTime average time: $averageTime min time: $minTime max time: $maxTime"
}