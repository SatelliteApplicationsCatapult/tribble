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

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

import org.apache.commons.io.IOUtils
import org.jacoco.core.analysis.{Analyzer, CoverageBuilder, IClassCoverage, ICounter}
import org.jacoco.core.data.{ExecutionDataStore, SessionInfoStore}
import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.runtime.{LoggerRuntime, RuntimeData}

import scala.collection.mutable


/**
  * Classloader that will automatically instrument classes as it is asked for.
  * Unless the classes start with one of the ignores prefixes.
  * Which default to java. javax. sun. org.catapult.sa.tribble.
  * Extra ones can be added with the addFilter(string) method
  */
class CoverageMemoryClassLoader(val parent : ClassLoader) extends ClassLoader(parent) {

  private val runtime = new LoggerRuntime

  private val data = new RuntimeData()
  runtime.startup(data)

  private val definitionClasses = new mutable.HashMap[String, Class[_]]()
  private val classBlobs = new mutable.HashMap[String, Array[Byte]]()
  private val instr = new Instrumenter(runtime)

  private val ignores = new mutable.ArrayBuffer[String](5)
  ignores.append("java.", "javax.", "sun.", "org.jacoco.", "org.catapult.sa.tribble.")

  /**
    * Pre load a class into this classloader
    * @param name name of the class.
    */
  def addClass(name : String) : Array[Byte] = {
    val classFile = CoverageMemoryClassLoader.getClassStream(name, parent)
    try {
      val fileContent = IOUtils.toByteArray(classFile)
      classBlobs.put(name, fileContent)
      instr.instrument(fileContent, name)
    } finally {
      classFile.close()
    }
  }

  /**
    * Add a filter for classes that should not be instrumented.
    * @param prefix of the classes to ignore.
    */
  def addFilter(prefix : String) : Unit = {
    ignores.append(prefix)
  }

  def reset() : Unit = {
    data.reset()
  }

  def shutdown() : Unit = {
    data.reset()
    runtime.shutdown()
  }

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    val clazz = definitionClasses.get(name)
    if (clazz.isDefined) {
      clazz.get
    } else {
      // we cant override the stuff in java. So don't try to instrument
      // Strange things happen if we try and instrument our selves. Don't
      if (shouldTryLoading(name)) {
        val instrumented = addClass(name)
        val result = defineClass(name, instrumented, 0, instrumented.length)
        definitionClasses.put(name, result)
        result
      } else {
        super.loadClass(name, resolve)
      }
    }
  }

  private def shouldTryLoading(name : String) : Boolean = {
    if (name.contains(".")) { // possibly a full path check our list of ignores
      !ignores.exists(name.startsWith)
    } else { // not a full path if might be something like String so we should not try and load.
      false
    }
  }

  def getDefinedClasses : Iterator[String] = definitionClasses.keysIterator

  // calculate an md5 hash of the line coverage generated using this memory classloader.
  def generateCoverageHash() : String = {

    val executionData = new ExecutionDataStore
    val sessionInfo = new SessionInfoStore
    data.collect(executionData, sessionInfo, true)


    val coverageBuilder = new CoverageBuilder
    val analyzer = new Analyzer(executionData, coverageBuilder)

    this.classBlobs.foreach(e => {
      // have to reload the classes here as we need the un-instrumented ones.
      analyzer.analyzeClass(e._2, e._1)
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

  /**
    * Get hold a stream for a class file.
    *
    * If you use this make sure you close the stream. If you don't call close on the stream object you will leak memory
    * as the stream used in the background to read the jar file will not be closed which will leave memory hanging around
    *
    * @param name class name to find
    * @param parent the class loader to use to find this class.
    * @return Inputstream that you can read the class from. Make sure you call close on this.
    */
  def getClassStream(name : String, parent : ClassLoader) : InputStream = {
    val res =  name.replace('.', '/') + ".class"
    val result = parent.getResourceAsStream(res)
    if (result == null) {
      val tryAgain = parent.getResourceAsStream("/" + res)
      if (tryAgain == null) {
        throw new IllegalArgumentException("Can not find class " + res)
      } else {
        tryAgain
      }
    } else {
      result
    }
  }
}
