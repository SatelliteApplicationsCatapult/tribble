package org.catapult.sa.tribble.stats

import java.io.PrintStream

abstract class StatsRender {
  def render(stats : CurrentStats) : Unit
}

class PrintStreamStatsRender(ps : PrintStream) extends StatsRender {
  override def render(stats: CurrentStats): Unit = {
    ps.println(stats)
  }
}

class StdErrStatsRender() extends PrintStreamStatsRender(System.err)
