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

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong

import org.apache.commons.lang.StringUtils
import org.catapult.sa.tribble.stats._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random


/**
  * Main implementation of fuzzing logic.
  */
class Fuzzer(corpus : Corpus,
             stats : Stats[_ <: RunDetails],
             ignoreClasses : Array[String] = Array(),
             threadCount : Int = 2,
             timeout : Long = 1000L,
             iterationCount : Long = -1,
             disabledMutators : Array[String] = Array()) {

  val rand = new Random()
  // TODO: argument for seed? Pretty sure this isn't needed with the saving of inputs and stacktraces.
  rand.setSeed(System.currentTimeMillis())

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
      stats.render()
    }

    val fails = futures.filter(_.get().isInstanceOf[Throwable])
    if (fails.isEmpty) {
      fails.foreach(f => f.get().asInstanceOf[Throwable].printStackTrace())
    }

  }

  private def fuzzLoop[T <: RunDetails](targetName : String,
                       coverageSet : ConcurrentHashMap[String, Object],
                       workQueue : BlockingQueue[(Array[Byte], String)],
                       stats : Stats[T],
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

        // mutate everything from the corpus.
        workQueue.addAll(drain.asScala.map(e => mutator.mutate(e._1)).asJavaCollection)
      }

      val old = workQueue.poll()
      if (old != null) { // possible race condition between the last two lines. Go around again if it happens. Scala has no continue keyword

        val start : T = stats.startRun()
        val (result, hash, ex) = runOnce(targetClass, old._1, memoryClassLoader)

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

        start.mutator = old._2
        start.success = FuzzResult.Passed(result)
        start.ignored = result == FuzzResult.IGNORE
        start.timeout = ex.exists(_.isInstanceOf[TimeoutException])
        start.newPath = result != FuzzResult.IGNORE && (!coverageSet.containsKey(hash) || result == FuzzResult.INTERESTING)
        // done here as this stops the timing and we don't care about the time taken to write the corpus entries.
        stats.finishRun(start)

        if (result == FuzzResult.FAILED) {
          corpus.saveResult(old._1, success = false, ex)
        }

        if (start.newPath) {
          coverageSet.put(hash, obj)

          if (FuzzResult.Passed(result)) {
            corpus.saveResult(old._1, success = true, ex) // only save on success, it would have been saved already on fail
          }

          val newInput = mutator.mutate(old._1)
          workQueue.put(newInput)
          start.newPath = true
        }

      }
    }

  }

  private def runOnce(targetClass : Class[_ <: FuzzTest], input : Array[Byte], memoryClassLoader : CoverageMemoryClassLoader) : (FuzzResult, String, Option[Throwable]) = {

    // Here we execute our test target class through its interface
    memoryClassLoader.reset()

    var targetInstance = targetClass.getDeclaredConstructor().newInstance()

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
