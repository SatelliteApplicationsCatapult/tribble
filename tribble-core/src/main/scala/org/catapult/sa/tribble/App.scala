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

package org.catapult.sa.tribble

import org.apache.commons.lang.StringUtils
import org.catapult.sa.tribble
/**
 * Entry point for command line usage of the tribble fuzz testing tool.
 */
object App extends Arguments {

  private val TARGET_CLASS = "targetClass"
  private val THREAD_COUNT = "threads"
  private val TIMEOUT = "timeout"
  private val CORPUS = "corpus"
  private val FAILED = "failed"
  private val COUNT = "count"
  private val IGNORECLASSES = "ignore"
  private val VERBOSE = "verbose"
  private val DISABLED = "disabledMutations"


  override def allowedArgs(): List[Argument] = List(
    Argument(CORPUS, "corpus"),
    Argument(FAILED, "failed"),
    Argument(THREAD_COUNT, "2"),
    Argument(TIMEOUT, "1000"),
    Argument(COUNT, "-1"),
    Argument(TARGET_CLASS),
    Argument(IGNORECLASSES),
    Argument(VERBOSE, "false", flag = true),
    Argument(DISABLED)
  )

  def main(args : Array[String]) : Unit = {

    val arguments : Map[String, String]  = processArgs(args)

    if (StringUtils.isBlank(arguments.getOrElse(TARGET_CLASS, ""))) {
      println("ERROR: targetClass must be set")
      return
    }

    if (!StringUtils.isNumeric(arguments.getOrElse(THREAD_COUNT, ""))) {
      println("ERROR: Thread count must be numeric")
      return
    }

    if (!StringUtils.isNumeric(arguments.getOrElse(TIMEOUT, ""))) {
      println("ERROR: timeout must be numeric")
      return
    }

    // TODO: fix -numbers
    /*if (!StringUtils.isNumeric(arguments.getOrElse(COUNT, "")) ) {
      println("ERROR: count must be numeric (" + arguments.getOrElse(COUNT, "") + ")")
      return
    }*/

    val fuzzer = new FuzzerFactory()
      .defaultStats(arguments(VERBOSE).toBoolean)
      .fileSystemCorpus(arguments(CORPUS), arguments(FAILED))
      .threads(arguments(THREAD_COUNT).toInt)
      .iterations(arguments(COUNT).toLong)
      .timeout(arguments(TIMEOUT).toLong)
      .disable(arguments(DISABLED).split(","):_*)
      .ignore(arguments(IGNORECLASSES).split(","):_*)
      .build()

    fuzzer.run(arguments(TARGET_CLASS), getClass.getClassLoader)
  }

}

