package org.catapult.sa.tribble.stats

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import scala.collection.mutable

/**
  * Keep track of the stats. Number of runs, number of crashes etc
  *
  * This is used for displaying a running log as the program is running.
  */
class Stats {

  private val runs  = new AtomicLong(0)
  private val fails = new AtomicLong(0)
  private val timeouts = new AtomicLong(0)
  private val paths = new AtomicInteger(0)
  private val totalTime = new AtomicLong(0)
  private val mutators = mutable.HashMap.empty[String, (AtomicLong, AtomicLong)]
  private val minTime = new AtomicLong(Long.MaxValue)
  private val maxTime = new AtomicLong(Long.MinValue)

  def addRun(success : Boolean, timeout: Boolean, newPath : Boolean, time : Long, mutator : String): Unit = {
    runs.incrementAndGet()
    if (!success) {
      if (timeout) {
        timeouts.incrementAndGet()
      } else {
        fails.incrementAndGet()
      }
    }

    if (newPath) {
      paths.incrementAndGet()
    }

    totalTime.addAndGet(time)

    val mutatorValue = mutators.getOrElseUpdate(mutator, (new AtomicLong(0), new AtomicLong(0)))
    mutatorValue._2.incrementAndGet()
    if (newPath) {
      mutatorValue._1.incrementAndGet()
    }

    mutators.update(mutator, mutatorValue)

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

  def getStats(printDetailedStats : Boolean) : CurrentStats = {
    val tt = totalTime.get()
    val r = runs.get()
    val avg = if (r == 0) 0 else tt/r
    val m = if (printDetailedStats) {
      mutators.map(a => a._1 -> (a._2._1.get() -> a._2._2.get())).toMap
    } else {
      Map.empty[String,(Long, Long)]
    }
    CurrentStats(r, fails.get(), timeouts.get(), paths.get(), tt, avg, minTime.get(), maxTime.get(), m, printDetailedStats)
  }

}



