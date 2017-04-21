package org.catapult.sa.tribble

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

import org.jacoco.core.analysis.{Analyzer, CoverageBuilder, IClassCoverage, ICounter}
import org.jacoco.core.data.{ExecutionDataStore, SessionInfoStore}
import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.runtime.{LoggerRuntime, RuntimeData}

import scala.collection.mutable


/**
  * Classloader that will automatically instrument classes as it is asked for.
  *
  * TODO: It might be worth keeping the same ClassLoader but resetting the stats some how. Investigate
  */
class CoverageMemoryClassLoader() extends ClassLoader {

  private val runtime = new LoggerRuntime

  private val data = new RuntimeData()
  runtime.startup(data)

  private val definitions = new mutable.HashMap[String, Array[Byte]]()
  private val instr = new Instrumenter(runtime)

  private val ignores = new mutable.ArrayBuffer[String](2)
  ignores.append("java.", "org.catapult.sa.tribble.")

  /**
    * Pre load a class into this classloader
    * @param name name of the class.
    */
  def addClass(name : String) : Unit = {
    val instrumented = instr.instrument(CoverageMemoryClassLoader.getClassStream(name), name)
    definitions.put(name, instrumented)
  }

  /**
    * Add a filter for classes that should not be instrumented.
    * @param prefix of the classes to ignore.
    */
  def addFilter(prefix : String) : Unit = {
    ignores.append(prefix)
  }


  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    val bytes = definitions.get(name)
    if (bytes.isDefined) {
      val b = bytes.get
      defineClass(name, b, 0, b.length)
    } else {
      // we cant override the stuff in java. So don't try to instrument
      // Strange things happen if we try and instrument our selves. Don't
      if (!ignores.exists(name.startsWith)) {
        addClass(name)
        val instrumented = definitions(name)
        defineClass(name, instrumented, 0, instrumented.length)
      } else {
        super.loadClass(name, resolve)
      }
    }
  }

  def getDefinedClasses : Iterator[String] = definitions.keysIterator

  // calculate an md5 hash of the line coverage generated using this memory classloader.
  def generateCoverageHash() : String = {

    val executionData = new ExecutionDataStore
    val sessionInfo = new SessionInfoStore
    data.collect(executionData, sessionInfo, false)
    runtime.shutdown()

    val coverageBuilder = new CoverageBuilder
    val analyzer = new Analyzer(executionData, coverageBuilder)

    this.getDefinedClasses.foreach(e => {
      // have to reload the classes here as we need the un-instrumented ones.
      analyzer.analyzeClass(CoverageMemoryClassLoader.getClassStream(e), e)
    })

    // make sure we are always generating the hash in the same order.
    val classes = this.getDefinedClasses.map(_.replace('.', '/')).toList.sorted

    val coverageMap = coverageBuilder.getClasses.toArray.map {
      case cc : IClassCoverage =>
        //println(cc.getName)
        cc.getName -> cc
    }.toMap[String, IClassCoverage]

    val md5 = MessageDigest.getInstance("MD5")
    classes.foreach( c => {
      coverageMap.get(c) match {
        case Some(cc) =>

          (cc.getFirstLine to cc.getLastLine).foreach(i => {
            val status = convertCover(cc.getLine(i).getStatus)
            //printf("%s:%d : %s\n", cc.getName, i, status)
            if (status != "Empty") {
              md5.update(s"${cc.getName}$i$status".getBytes(StandardCharsets.UTF_8))
            }
          })
        case _ => // ignore
      }
    })

    DatatypeConverter.printHexBinary(md5.digest())
  }

  private def convertCover(status : Int) : String = status match {
    case ICounter.EMPTY => "Empty"
    case ICounter.NOT_COVERED => "Not Covered"
    case ICounter.PARTLY_COVERED => "Partial cover"
    case ICounter.FULLY_COVERED => "Fully Covered"
  }

}

object CoverageMemoryClassLoader {
  def getClassStream(name : String) : InputStream = {
    val res = '/' + name.replace('.', '/') + ".class"
    getClass.getResourceAsStream(res)
  }
}