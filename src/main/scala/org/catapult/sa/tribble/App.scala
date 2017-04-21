package org.catapult.sa.tribble

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.`type`.filter.AssignableTypeFilter

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random
/**
 * Hello world! with calculated coverage
 * TODO: More complex test case.
 *
 */
object App extends Arguments {

  private val TARGET_CLASS = "targetClass"
  private val TARGET_PATH = "targetPath"

  override def allowedArgs(): List[Argument] = List(
    Argument(Corpus.CORPUS, "corpus"),
    Argument(Corpus.FAILED, "failed"),
    Argument(TARGET_CLASS),
    Argument(TARGET_PATH)
  )

  def main(args : Array[String]) : Unit = {

    arguments = processArgs(args)

    if (StringUtils.isBlank(arguments.getOrElse(TARGET_CLASS, "")) &&
      StringUtils.isBlank(arguments.getOrElse(TARGET_PATH, ""))) {
      println("ERROR: One of targetClass or targetPath must be set")
      return
    }

    if (! Corpus.validateDirectories(arguments)) {
      return
    }

    // TODO: Argument for seed? -- Not sure having saved problems it should be ok.
    rand.setSeed(System.currentTimeMillis())

    val targetName = if (StringUtils.isNotBlank(arguments.getOrElse(TARGET_CLASS, ""))) {
      arguments.get(TARGET_CLASS)
    } else {
      println("Class path scanning for instances of FuzzTest in " + arguments(TARGET_PATH))
      val scanner = new ClassPathScanningCandidateComponentProvider(true)
      scanner.addIncludeFilter(new AssignableTypeFilter(classOf[FuzzTest]))

      // Not sure why the implicit conversation is not kicking in here but this works.
      asScalaSet(scanner.findCandidateComponents(arguments(TARGET_PATH))).map((bd : BeanDefinition) => {
        println(bd.getBeanClassName)
        bd.getBeanClassName
      }).head
    }

    fuzzWithThreads(targetName.toString)
  }

  private var arguments : Map[String, String] = _

  lazy val rand = new Random()

  private def fuzzWithThreads(targetName : String) : Unit = {
    val coverageSet = new mutable.HashSet[String]()
    val stats = new Stats()

    // TODO: Threads / Thread pool

    val workStack = new mutable.ArrayStack[Array[Byte]]()
    workStack.push(Array[Byte]())
    Corpus.readCorpusInputStack(arguments, workStack)

    // TODO : Thread count option
    val pool: ExecutorService = Executors.newFixedThreadPool(2)

    (0 to 2).foreach( _ => {
      pool.submit(new Runnable() {
        def run() : Unit = fuzzLoop(targetName, coverageSet, workStack, stats)
      })
    })


    while (true) {
      pool.awaitTermination(5, TimeUnit.SECONDS)
      println(stats.getStats())
    }


  }

  private def fuzzLoop(targetName : String,
                       coverageSet : mutable.HashSet[String],
                       workStack : mutable.ArrayStack[Array[Byte]],
                       stats : Stats): Unit = {

    def loop() {

      if (workStack.isEmpty) {
        Corpus.readCorpusInputStack(arguments, workStack)
      }

      val old = workStack.pop()

      val newInput = Corpus.mutate(old, rand)
      val (result, hash, ex) = runOnce(targetName, newInput)
      if (!coverageSet.contains(hash)) {
        coverageSet.add(hash)

        Corpus.saveResult(newInput, result, ex, arguments)
        if (result) {
          workStack.push(newInput)
        }
        stats.addRun(success = result, newPath = true)
      } else {
        stats.addRun(success = result, newPath = false)
      }

      loop()
    }
    loop()
  }

  private def runOnce(targetName : String, input : Array[Byte]) : (Boolean, String, Option[Throwable]) = {

    val memoryClassLoader = new CoverageMemoryClassLoader()
    memoryClassLoader.addClass(targetName)

    val targetClass = memoryClassLoader.loadClass(targetName)

    // Here we execute our test target class through its interface
    val targetInstance = targetClass.newInstance.asInstanceOf[FuzzTest]
    try {
      result = targetInstance.test(input)
      (true, memoryClassLoader.generateCoverageHash(), None)
    } catch {
      case e : Throwable =>
        (false, memoryClassLoader.generateCoverageHash(), Some(e))
    }


  }


}

