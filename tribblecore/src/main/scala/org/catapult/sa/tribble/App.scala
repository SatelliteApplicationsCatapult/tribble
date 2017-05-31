package org.catapult.sa.tribble

import org.apache.commons.lang.StringUtils
/**
 *
  * Entry point for command line usage of the tribble fuzz testing tool.
  *
 * TODO: Iteration count parameter.
 */
object App extends Arguments {

  private val TARGET_CLASS = "targetClass"
  private val THREAD_COUNT = "threads"
  private val CORPUS = "corpus"
  private val FAILED = "failed"

  override def allowedArgs(): List[Argument] = List(
    Argument(CORPUS, "corpus"),
    Argument(FAILED, "failed"),
    Argument(THREAD_COUNT, "2"),
    Argument(TARGET_CLASS)
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

    if (! Corpus.validateDirectories(arguments(CORPUS), arguments(FAILED))) {
      return
    }

    val fuzzer = new Fuzzer(arguments(CORPUS), arguments(FAILED), arguments.getOrElse(THREAD_COUNT, "2").toInt)

    fuzzer.run(arguments(TARGET_CLASS), getClass.getClassLoader)
  }

}

