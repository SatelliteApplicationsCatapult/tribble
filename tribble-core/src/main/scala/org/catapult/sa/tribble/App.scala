package org.catapult.sa.tribble

import org.apache.commons.lang.StringUtils
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


  override def allowedArgs(): List[Argument] = List(
    Argument(CORPUS, "corpus"),
    Argument(FAILED, "failed"),
    Argument(THREAD_COUNT, "2"),
    Argument(TIMEOUT, "1000"),
    Argument(COUNT, "-1"),
    Argument(TARGET_CLASS),
    Argument(IGNORECLASSES),
    Argument(VERBOSE, "false", flag = true)
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

    if (! Corpus.validateDirectories(arguments(CORPUS), arguments(FAILED))) {
      return
    }

    val fuzzer = new Fuzzer(
      arguments(CORPUS),
      arguments(FAILED),
      arguments(IGNORECLASSES).split(","),
      arguments(THREAD_COUNT).toInt,
      arguments(TIMEOUT).toLong,
      arguments(COUNT).toLong,
      arguments(VERBOSE).toBoolean
    )

    fuzzer.run(arguments(TARGET_CLASS), getClass.getClassLoader)
  }

}

