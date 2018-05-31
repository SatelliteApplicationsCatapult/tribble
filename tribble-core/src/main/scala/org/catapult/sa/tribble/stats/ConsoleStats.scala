/*
 *    Copyright 2018 Satellite Applications Catapult Limited.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.catapult.sa.tribble.stats

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import scala.collection.mutable

case class ConsoleRunDetails(startTime : Long) extends RunDetails

class ConsoleStats(printDetailedStats : Boolean) extends Stats[ConsoleRunDetails] {

  private val runs  = new AtomicLong(0)
  private val fails = new AtomicLong(0)
  private val ignored = new AtomicLong(0)
  private val timeouts = new AtomicLong(0)
  private val paths = new AtomicInteger(0)
  private val totalTime = new AtomicLong(0)
  private val mutators = mutable.HashMap.empty[String, (AtomicLong, AtomicLong)]
  private val minTime = new AtomicLong(Long.MaxValue)
  private val maxTime = new AtomicLong(Long.MinValue)

  override def startRun(): ConsoleRunDetails = {
    ConsoleRunDetails(
      startTime = System.currentTimeMillis()
    )
  }

  override def finishRun(start: ConsoleRunDetails): Unit = {

    val time = System.currentTimeMillis() - start.startTime

    runs.incrementAndGet()
    if (!start.success) {
      if (start.timeout) {
        timeouts.incrementAndGet()
      } else if (start.ignored) {
        ignored.incrementAndGet()
      } else {
        fails.incrementAndGet()
      }
    }

    if (start.newPath) {
      paths.incrementAndGet()
    }

    totalTime.addAndGet(time)

    val mutatorValue = mutators.getOrElseUpdate(start.mutator, (new AtomicLong(0), new AtomicLong(0)))
    mutatorValue._2.incrementAndGet()
    if (start.newPath) {
      mutatorValue._1.incrementAndGet()
    }

    mutators.update(start.mutator, mutatorValue)

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

  override def render(): Unit = {
    System.err.println(getStats.toString)
  }

  private def getStats : CurrentStats = {
    val tt = totalTime.get()
    val r = runs.get()
    val avg = if (r == 0) 0 else tt/r
    val m = if (printDetailedStats) {
      mutators.map(a => a._1 -> (a._2._1.get() -> a._2._2.get())).toMap
    } else {
      Map.empty[String,(Long, Long)]
    }
    CurrentStats(r, fails.get(), timeouts.get(), paths.get(), ignored.get(), tt, avg, minTime.get(), maxTime.get(), m, printDetailedStats)
  }

}

case class CurrentStats(runs : Long, fails : Long, timeouts : Long, paths : Int, ignore : Long, totalTime : Long, averageTime : Long, minTime : Long, maxTime : Long, mutators : Map[String, (Long, Long)], printDetailedStats : Boolean) {
  override def toString: String = {
    val t = CurrentStats.formatDuration(totalTime)
    val a = CurrentStats.formatDuration(averageTime)
    val min = if (minTime == Long.MaxValue) "~" else CurrentStats.formatDuration(minTime)
    val max = if (maxTime == Long.MinValue) "~" else CurrentStats.formatDuration(maxTime)

    val mutatorText = if (printDetailedStats) {
      val mutatorStats = mutators.map { a =>
        val ratio = a._2._1.toDouble / a._2._2.toDouble
        s"${a._1},${a._2._1},${a._2._2},$ratio"
      }.mkString("\n")
      val mutatorHeader = "mutator,paths,runs,ratio"
      s"\n$mutatorHeader\n$mutatorStats"
    } else {
      ""
    }

    s"runs: $runs fails: $fails timeouts: $timeouts paths: $paths ignored: $ignore total time: $t average time: $a min time: $min max time: $max$mutatorText"
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
