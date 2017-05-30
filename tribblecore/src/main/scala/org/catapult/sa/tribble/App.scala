package org.catapult.sa.tribble

import java.util.concurrent._

import org.apache.commons.lang.StringUtils

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random
/**
 *
 * TODO: More complex test case.
 * TODO: Iteration count parameter.
 * TODO: Maven plugin.
 *
 */
object App extends Arguments {

  private val TARGET_CLASS = "targetClass"
  private val THREAD_COUNT = "threads"

  override def allowedArgs(): List[Argument] = List(
    Argument(Corpus.CORPUS, "corpus"),
    Argument(Corpus.FAILED, "failed"),
    Argument(THREAD_COUNT, "2"),
    Argument(TARGET_CLASS)
  )

  def main(args : Array[String]) : Unit = {

    setArgs(args)

    if (StringUtils.isBlank(arguments.getOrElse(TARGET_CLASS, ""))) {
      println("ERROR: targetClass must be set")
      return
    }

    if (!StringUtils.isNumeric(arguments.getOrElse(THREAD_COUNT, ""))) {
      println("ERROR: Thread count must be numeric")
      return
    }

    if (! Corpus.validateDirectories(arguments)) {
      return
    }

    // TODO: Argument for seed? -- Not sure having saved problems it should be ok.
    rand.setSeed(System.currentTimeMillis())

    val targetName = arguments(TARGET_CLASS)

    fuzzWithThreads(targetName.toString, this.getClass.getClassLoader)
  }

  var arguments : Map[String, String] = _

  def setArgs(args : Array[String]) : Unit = {
    arguments = processArgs(args)
  }

  lazy val rand = new Random()

  def fuzzWithThreads(targetName : String, cl : ClassLoader) : Unit = {
    val coverageSet = new ConcurrentHashMap[String, Object]()

    val stats = new Stats()

    val workStack = new LinkedBlockingQueue[Array[Byte]]()
    workStack.put(Array[Byte]()) // first time through always try with an empty array just in case the corpus is empty
    Corpus.readCorpusInputStack(arguments, workStack)


    val pool: ExecutorService = Executors.newFixedThreadPool(arguments.getOrElse(THREAD_COUNT, "2").toInt, (r: Runnable) => {
      val result = new Thread(r)
      result.setContextClassLoader(cl)
      result
    })

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

    var (memoryClassLoader, targetClass) = createClassLoader(targetName)

    var pathCountLastLoad = 0
    val obj = new Object() // don't create a new object for every entry in our hash "set"
    while(true) {

      if (workQueue.isEmpty) {
        Corpus.readCorpusInputStack(arguments, workQueue)
        if (pathCountLastLoad == coverageSet.size()) {
          // System.err.println("No change from last reset point. Performing large mutation.")
          // the last run did not generate any new coverages. Really randomise the corpus this time.
          val drain = new java.util.ArrayList[Array[Byte]]()
          workQueue.drainTo(drain)
          // mutate lots of times and try again
          workQueue.addAll(asScalaBuffer(drain).map(e => (0 to 50).foldLeft(e)((a, _) => Corpus.mutate(a, rand))).asJavaCollection)
        }
        pathCountLastLoad = coverageSet.size()

      }

      val old = workQueue.poll()

      val start = System.currentTimeMillis()
      val (result, hash, ex) = runOnce(targetClass, old, memoryClassLoader)
      val totalTime = System.currentTimeMillis() - start

      // If we had an error and its an out of memory error.
      // Kill off the class and the classloader. Manually GC and then recreate the classloader.
      if (ex.isDefined) {
        if (ex.get.isInstanceOf[OutOfMemoryError]) {
          targetClass = null
          memoryClassLoader = null

          System.gc()
          val (newMemoryClassLoader, newTargetClass) = createClassLoader(targetName)
          memoryClassLoader = newMemoryClassLoader
          targetClass = newTargetClass
        }
      }

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

    var targetInstance = targetClass.newInstance()

    try {

      implicit val context = ExecutionContext.global
      Await.result(Future {
        try {
          val result = targetInstance.test(input)
          (result, memoryClassLoader.generateCoverageHash(), None)
        } catch {
          case e: OutOfMemoryError => // Out of memory. Clean up what we can and force a GC before handing back up the stack
            targetInstance = null
            System.gc()
            (false, memoryClassLoader.generateCoverageHash(), Some(e))
          case e: Throwable =>
            (false, memoryClassLoader.generateCoverageHash(), Some(e))
        }

      }, Duration(1000L, TimeUnit.MILLISECONDS))
    } catch {
      case e : TimeoutException => (false, memoryClassLoader.generateCoverageHash(), Some(e))
    }
  }

  private def createClassLoader[T <: FuzzTest](targetName : String) : (CoverageMemoryClassLoader, Class[T]) = {
    val memoryClassLoader = new CoverageMemoryClassLoader(Thread.currentThread().getContextClassLoader)

    memoryClassLoader.addClass(targetName)
    val targetClass = memoryClassLoader.loadClass(targetName).asInstanceOf[Class[T]]

    Thread.currentThread().setContextClassLoader(memoryClassLoader)
    (memoryClassLoader, targetClass)
  }

}

