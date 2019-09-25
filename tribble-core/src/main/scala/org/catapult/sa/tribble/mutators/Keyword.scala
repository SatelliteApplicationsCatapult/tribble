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

package org.catapult.sa.tribble.mutators

import java.net.URL
import java.nio.charset.StandardCharsets

import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils

import scala.collection.JavaConverters._
import scala.util.Random

class Keyword extends Mutator {
  private val inputList = loadLists()

  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    // pick a random spot inside the array and splice in a random keyword.
    val target = rand.nextInt(in.length)
    val word = inputList(rand.nextInt(inputList.length))

    val (start, end) = in.splitAt(target)
    Array.concat(start, word.getBytes(StandardCharsets.UTF_8), end)
  }

  private def loadLists() : List[String] = {
    val classLoader = getClass.getClassLoader
    classLoader.getResources("org.catapult.sa.tribble.keywords").asScala
      .flatMap(loadList).toList
  }

  private def loadList(u: URL) : List[String] = {
    val stream = u.openStream()
    IOUtils.readLines(stream, StandardCharsets.UTF_8).asScala.filter(StringUtils.isNotEmpty).toList
  }
}
