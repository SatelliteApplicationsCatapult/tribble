package org.catapult.sa.tribble

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

  def getStats : CurrentStats = {
    val tt = totalTime.get()
    val r = runs.get()
    val avg = if (r == 0) 0 else tt/r
    val m = mutators.map(a => a._1 -> (a._2._1.get() -> a._2._2.get())).toMap
    CurrentStats(r, fails.get(), timeouts.get(), paths.get(), tt, avg, minTime.get(), maxTime.get(), m)
  }

}

case class CurrentStats(runs : Long, fails : Long, timeouts : Long, paths : Int, totalTime : Long, averageTime : Long, minTime : Long, maxTime : Long, mutators : Map[String, (Long, Long)]) {
  override def toString: String = {
    val t = CurrentStats.formatDuration(totalTime)
    val a = CurrentStats.formatDuration(averageTime)
    val min = if (minTime == Long.MaxValue) "~" else CurrentStats.formatDuration(minTime)
    val max = if (maxTime == Long.MinValue) "~" else CurrentStats.formatDuration(maxTime)

    val mutatorStats = mutators.map { a =>
      val ratio = a._2._1.toDouble / a._2._2.toDouble
      s"${a._1},${a._2._1},${a._2._2},${ratio}"
    }.mkString("\n")
    val mutatorHeader = "mutator,paths,runs,ratio"

    s"runs: $runs fails: $fails timeouts: $timeouts paths: $paths total time: $t average time: $a min time: $min max time: $max\n$mutatorHeader\n$mutatorStats"
  }
}

object CurrentStats {
  // Java8 Time doesn't have a duration formatter. Blah
  def formatDuration(d : Long) : String = {
    if (d == 0) {
      "0S"
    } else {
      val abs = Math.abs(d)
      val seconds = abs / 1000
      val millis = abs - (seconds * 1000)

      ((if (seconds/3600 > 0) (seconds/3600) + "H " else "") +
        (if ((seconds%3600)/60 > 0) (seconds%3600)/60 + "M " else "") +
        (if (seconds%60 > 0) (seconds%60) + "S " else "") +
        (if (millis > 0) millis + "ms" else "")).trim
    }
  }
}