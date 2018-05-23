package org.catapult.sa.tribble.stats

case class CurrentStats(runs : Long, fails : Long, timeouts : Long, paths : Int, totalTime : Long, averageTime : Long, minTime : Long, maxTime : Long, mutators : Map[String, (Long, Long)], printDetailedStats : Boolean) {
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

    s"runs: $runs fails: $fails timeouts: $timeouts paths: $paths total time: $t average time: $a min time: $min max time: $max$mutatorText"
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