package org.catapult.sa.tribble

import java.io.InputStream

import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.runtime.AbstractRuntime

import scala.collection.mutable


/**
  * Classloader that will automatically instrument classes as it is asked for.
  */
class CoverageMemoryClassLoader(runtime : AbstractRuntime) extends ClassLoader {

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

  def getDefinedClasses : Map[String, Array[Byte]] = {
    definitions.toMap
  }



}

object CoverageMemoryClassLoader {
  def getClassStream(name : String) : InputStream = {
    val res = '/' + name.replace('.', '/') + ".class"
    getClass.getResourceAsStream(res)
  }
}