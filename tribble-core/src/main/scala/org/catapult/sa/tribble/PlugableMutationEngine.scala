package org.catapult.sa.tribble

import java.net.URL
import java.nio.charset.StandardCharsets

import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.catapult.sa.tribble.mutators.Mutator

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.Random

/**
  * Extensible mutation engine. Looks for files on the classpath called "org.catapult.sa.tribble.mutators"
  * Inside it expects to find a list of fully qualified class names which it will load as Mutators.
  *
  * This should make it very easy to add new experimental mutation strategies
  */
class PlugableMutationEngine(rand: Random) extends MutationEngine {

  private val mutators = findClasses()
  private val mutatorsNames = mutators.map(_.getClass.getSimpleName)

  private def findClasses(): List[Mutator] = {
    val classLoader = getClass.getClassLoader
    classLoader.getResources("org.catapult.sa.tribble.mutators")
      .flatMap(loadFile(_, classLoader))
      .map(_.newInstance())
      .toList
  }

  override def mutate(input: Array[Byte]): (Array[Byte], String) = {
    if (input == null || input.isEmpty) {
      (Array[Byte](0x00), "NullDefault")
    } else {
      val index = rand.nextInt(mutators.length)
      (mutators(index).mutate(input, rand), mutatorsNames(index))
    }
  }

  private def loadFile(u: URL, classLoader: ClassLoader): mutable.Buffer[Class[_ <: Mutator]] = {
    val stream = u.openStream()
    val result = IOUtils.readLines(stream, StandardCharsets.UTF_8).flatMap { l =>
      if (StringUtils.isNotBlank(l) || l.startsWith("#")) {
        loadClass(l, classLoader)
      } else {
        List.empty[Class[_ <: Mutator]]
      }
    }

    stream.close()
    result
  }

  private def loadClass(l: String, classLoader: ClassLoader): List[Class[_ <: Mutator]] = {
    try {
      val c = classLoader.loadClass(l)
      if (classOf[Mutator].isAssignableFrom(c)) {
        List(c.asInstanceOf[Class[_ <: Mutator]])
      } else {
        println(s"Could not load class $l it is not a mutator")
        List.empty[Class[_ <: Mutator]]
      }
    } catch {
      case e: ClassNotFoundException =>
        println(s"Could not load class $l")
        e.printStackTrace()
        List.empty[Class[_ <: Mutator]]
    }
  }
}
