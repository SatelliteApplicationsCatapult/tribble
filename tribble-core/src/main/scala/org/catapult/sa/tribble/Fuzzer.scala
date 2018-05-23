package org.catapult.sa.tribble

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong

import org.apache.commons.lang.StringUtils

import scala.collection.JavaConverters._
import scala.collection.convert.WrapAsScala.asScalaBuffer
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Random


/**
  * Main implementation of fuzzing logic.
  */
class Fuzzer(corpusPath : String = "corpus",
             failedPath : String = "failed",
             ignoreClasses : Array[String] = Array(),
             threadCount : Int = 2,
             timeout : Long = 1000L,
             iterationCount : Long = -1,
             printDetailedStats: Boolean = true,
             disabledMutators: Array[String] = Array()) {

  val rand = new Random()
  // TODO: argument for seed? Pretty sure this isn't needed with the saving of inputs and stacktraces.
  rand.setSeed(System.currentTimeMillis())

  // TODO: other options here.
  // HDFS/Database etc wait until these are needed.
  // In-Memory for tests
  val corpus : Corpus = new FileSystemCorpus(corpusPath, failedPath)

  private val countDown = if (iterationCount > 0)  {
    new AtomicLong(iterationCount)
  } else {
    null
  }

  def run(targetName : String, cl : ClassLoader) : Unit = {

    if (!corpus.validate()) {
      return
    }

    val coverageSet = new ConcurrentHashMap[String, Object]()

    val stats = new Stats()

    val workStack = new LinkedBlockingQueue[(Array[Byte], String)]()
    workStack.put((Array[Byte](), "Initial Value")) // first time through always try with an empty array just in case the corpus is empty
    corpus.readCorpus(workStack)

    val pool: ExecutorService = Executors.newFixedThreadPool(threadCount, new ThreadFactory {
      override def newThread(r: Runnable): Thread = {
        val result = new Thread(r)
        result.setContextClassLoader(cl)
        result
      }
    })

    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler {
      override def uncaughtException(t: Thread, e: Throwable): Unit = {
        System.err.println("Exception thrown in thread " + t.getName)
        e.printStackTrace(System.err)
      }
    })

    val futures = (0 to threadCount).map( _ => {
      pool.submit(new Runnable() {
        def run() : Unit = fuzzLoop(targetName, coverageSet, workStack, stats, cl)
      })
    }).toList

    pool.shutdown()

    while(futures.exists(!_.isDone)) {
      pool.awaitTermination(5, TimeUnit.SECONDS)
      System.err.println(stats.getStats(printDetailedStats))
    }

    val fails = futures.filter(_.get().isInstanceOf[Throwable])
    if (fails.isEmpty) {
      fails.foreach(f => f.get().asInstanceOf[Throwable].printStackTrace())
    }

  }

  private def fuzzLoop(targetName : String,
                       coverageSet : ConcurrentHashMap[String, Object],
                       workQueue : BlockingQueue[(Array[Byte], String)],
                       stats : Stats,
                       parent : ClassLoader): Unit = {

    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler {
      override def uncaughtException(t: Thread, e: Throwable): Unit = {
        System.err.println("Exception thrown in thread " + t.getName)
        e.printStackTrace(System.err)
      }
    })

    var (memoryClassLoader, targetClass) = createClassLoader(targetName, parent)

    var pathCountLastLoad = 0
    val obj = new Object() // don't create a new object for every entry in our hash "set"

    val mutator = new PlugableMutationEngine(rand, disabledMutators)

    while(countDown == null || countDown.getAndDecrement() > 0) {

      if (workQueue.isEmpty) {
        corpus.readCorpus(workQueue)

        val drain = new java.util.ArrayList[(Array[Byte], String)]()
        workQueue.drainTo(drain)

        if (pathCountLastLoad == coverageSet.size()) {

          // the last run did not generate any new coverages. Really randomise the corpus this time.
          // mutate lots of times and try again
          // Note: asScalaBuffer is deprecated in scala 2.12 however for now we need to support 2.11 which doesn't have the same class.
          workQueue.addAll(asScalaBuffer(drain).map(e => (0 to 50).foldLeft(e)((a, _) => mutator.mutate(a._1))).map(_._1 -> "big").asJavaCollection)
        } else {
          // mutate everything from the corpus.
          workQueue.addAll(asScalaBuffer(drain).map(e => mutator.mutate(e._1)).asJavaCollection)
        }
        pathCountLastLoad = coverageSet.size()
      }

      val old = workQueue.poll()
      if (old != null) { // possible race condition between the last two lines. Go around again if it happens. Scala has no continue keyword

        val start = System.currentTimeMillis()
        val (result, hash, ex) = runOnce(targetClass, old._1, memoryClassLoader)
        val totalTime = System.currentTimeMillis() - start

        // If we had an error and its an out of memory error.
        // Kill off the class and the classloader. Manually GC and then recreate the classloader.
        if (ex.isDefined && ex.get.isInstanceOf[OutOfMemoryError]) {
          targetClass = null
          memoryClassLoader.shutdown()
          memoryClassLoader = null

          System.gc()
          val (newMemoryClassLoader, newTargetClass) = createClassLoader(targetName, parent)
          memoryClassLoader = newMemoryClassLoader
          targetClass = newTargetClass
        }

        val wasTimeout = ex.exists(_.isInstanceOf[TimeoutException])

        if (result == FuzzResult.FAILED) {
          corpus.saveResult(old._1, false, ex)
        }

        if (result != FuzzResult.IGNORE && (!coverageSet.containsKey(hash) || result == FuzzResult.INTERESTING)) {
          coverageSet.put(hash, obj)

          if (FuzzResult.Passed(result)) {
            corpus.saveResult(old._1, true, ex) // only save on success, it would have been saved already on fail
          }

          val newInput = mutator.mutate(old._1)
          workQueue.put(newInput)
          stats.addRun(success = FuzzResult.Passed(result), timeout = wasTimeout, newPath = true, totalTime, old._2)
        } else {
          stats.addRun(success = FuzzResult.Passed(result), timeout = wasTimeout, newPath = false, totalTime, old._2)
        }
      }
    }

  }

  private def runOnce(targetClass : Class[_ <: FuzzTest], input : Array[Byte], memoryClassLoader : CoverageMemoryClassLoader) : (FuzzResult, String, Option[Throwable]) = {

    // Here we execute our test target class through its interface
    memoryClassLoader.reset()

    var targetInstance = targetClass.newInstance()

    try {

      implicit val context : ExecutionContext = ExecutionContext.global
      Await.result(Future {
        try {
          val result = targetInstance.test(input)
          (result, memoryClassLoader.generateCoverageHash(), None)
        } catch {
          case e: OutOfMemoryError => // Out of memory. Clean up what we can and force a GC before handing back up the stack
            targetInstance = null
            System.gc()
            (FuzzResult.FAILED, memoryClassLoader.generateCoverageHash(), Some(e))
          case e: Throwable =>
            (FuzzResult.FAILED, memoryClassLoader.generateCoverageHash(), Some(e))
        }

      }, Duration(timeout, TimeUnit.MILLISECONDS))
    } catch {
      case e : TimeoutException => (FuzzResult.FAILED, memoryClassLoader.generateCoverageHash(), Some(e))
    }
  }

  private def createClassLoader[T <: FuzzTest](targetName : String, parent : ClassLoader) : (CoverageMemoryClassLoader, Class[T]) = {
    val memoryClassLoader = new CoverageMemoryClassLoader(parent)

    ignoreClasses.filter(StringUtils.isNotBlank).foreach(memoryClassLoader.addFilter)
    memoryClassLoader.addClass(targetName)

    val targetClass = memoryClassLoader.loadClass(targetName).asInstanceOf[Class[T]]

    Thread.currentThread().setContextClassLoader(memoryClassLoader)
    (memoryClassLoader, targetClass)
  }

}
