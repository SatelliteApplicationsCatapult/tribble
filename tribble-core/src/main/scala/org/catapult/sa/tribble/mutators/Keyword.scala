package org.catapult.sa.tribble.mutators
import java.net.URL
import java.nio.charset.StandardCharsets
import scala.collection.JavaConverters._
import org.apache.commons.io.IOUtils

import scala.util.Random

class Keyword extends Mutator {
  private val inputList = loadLists()

  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    // pick a random spot inside the array and splice in a random keyword.
    val target = rand.nextInt(in.length)
    val word = inputList(rand.nextInt(inputList.length))

    val (start, end) = in.splitAt(target)
    start ++ word.getBytes(StandardCharsets.UTF_8) ++ end
  }

  private def loadLists() : List[String] = {
    val classLoader = getClass.getClassLoader
    classLoader.getResources("org.catapult.sa.tribble.keywords").asScala
      .flatMap(loadList).toList
  }

  private def loadList(u: URL) : List[String] = {
    val stream = u.openStream()
    IOUtils.readLines(stream, StandardCharsets.UTF_8).asScala.toList
  }
}
