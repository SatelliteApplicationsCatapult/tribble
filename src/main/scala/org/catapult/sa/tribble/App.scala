package org.catapult.sa.tribble

import java.util.concurrent._

import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.`type`.filter.AssignableTypeFilter

import scala.collection.JavaConverters._
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
    val coverageSet = new ConcurrentHashMap[String, Object]()

    val stats = new Stats()

    val workStack = new LinkedBlockingQueue[Array[Byte]]()
    workStack.put(Array[Byte]()) // first time through always try with an empty array just in case the corpus is empty
    Corpus.readCorpusInputStack(arguments, workStack)

    // TODO : Thread count option
    val pool: ExecutorService = Executors.newFixedThreadPool(1)

    Thread.setDefaultUncaughtExceptionHandler((t, e) => {
      System.err.println("Exception thrown in thread " + t.getName)
      e.printStackTrace(System.err)
    })

    val futures = (0 to 2).map( _ => {
      pool.submit(new Runnable() {
        def run() : Unit = fuzzLoop(targetName, coverageSet, workStack, stats)
      })
    }).toList

    pool.shutdown()

    while(futures.exists(!_.isDone)) {
      pool.awaitTermination(5, TimeUnit.SECONDS)
      System.err.println(stats.getStats)
    }

    val fails = futures.filter(_.get().isInstanceOf[Throwable])
    if (fails.isEmpty) {
      fails.foreach(f => f.get().asInstanceOf[Throwable].printStackTrace())
    }

  }

  private def fuzzLoop(targetName : String,
                       coverageSet : ConcurrentHashMap[String, Object],
                       workQueue : BlockingQueue[Array[Byte]],
                       stats : Stats): Unit = {

    Thread.currentThread().setUncaughtExceptionHandler((t, e) => {
      System.err.println("Exception thrown in thread " + t.getName)
      e.printStackTrace(System.err)
    })

    val memoryClassLoader = new CoverageMemoryClassLoader()

    memoryClassLoader.addClass(targetName)
    val targetClass = memoryClassLoader.loadClass(targetName).asInstanceOf[Class[_ <: FuzzTest]]

    Thread.currentThread().setContextClassLoader(memoryClassLoader)

    val obj = new Object() // don't create a new object for every entry in our hash "set"
    while(true) {

      if (workQueue.isEmpty) {
        Corpus.readCorpusInputStack(arguments, workQueue)
      }

      val old = workQueue.poll()

      val start = System.currentTimeMillis()
      val (result, hash, ex) = runOnce(targetClass, old, memoryClassLoader)
      val totalTime = System.currentTimeMillis() - start

      if (!coverageSet.containsKey(hash)) {
        coverageSet.put(hash, obj)
        Corpus.saveResult(old, result, ex, arguments)
        if (result) {
          val newInput = Corpus.mutate(old, rand)
          workQueue.put(newInput)
        }
        stats.addRun(success = result, newPath = true, totalTime)
      } else {
        stats.addRun(success = result, newPath = false, totalTime)
      }
    }

  }

  private def runOnce(targetClass : Class[_ <: FuzzTest], input : Array[Byte], memoryClassLoader : CoverageMemoryClassLoader) : (Boolean, String, Option[Throwable]) = {

    // Here we execute our test target class through its interface
    memoryClassLoader.reset()

    val targetInstance = targetClass.newInstance
    try {
      val result = targetInstance.test(input)
      (result, memoryClassLoader.generateCoverageHash(), None)
    } catch {
      case e : Throwable =>
        (false, memoryClassLoader.generateCoverageHash(), Some(e))
    }


  }


}

