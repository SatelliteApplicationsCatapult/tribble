package org.catapult.sa.tribble

import java.io.{File, FileInputStream, FileOutputStream, PrintStream}
import java.nio.file.{Files, Paths}
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.`type`.filter.AssignableTypeFilter

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random
/**
 * Hello world! with calculated coverage
  *
  * TODO: More complex test case.
 *
 */
object App extends Arguments {

  private val CORPUS = "corpus"
  private val FAILED = "failed"
  private val TARGET_CLASS = "targetClass"
  private val TARGET_PATH = "targetPath"

  override def allowedArgs(): List[Argument] = List(
    Argument(CORPUS, "corpus"),
    Argument(FAILED, "failed"),
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

    val corpusFile = new File(arguments(CORPUS))
    if (corpusFile.exists() && (! corpusFile.isDirectory || !corpusFile.canWrite)) {
      println("ERROR: corpus path exists but is not a directory: " + corpusFile.getAbsolutePath)
      return
    }

    if (!corpusFile.exists()) {
      println("Corpus folder does not exist. Creating. WARNING you should give the tribbles some guidance where to start.")
      Files.createDirectory(Paths.get(arguments(CORPUS)))
    }

    val failedFile = new File(arguments(FAILED))
    if (failedFile.exists() && (! failedFile.isDirectory || !failedFile.canWrite)) {
      println("ERROR: failed path exists but is not a directory: " + failedFile.getAbsolutePath)
      return
    }

    if (! failedFile.exists()) {
      println("Failed directory does not exist. Creating.")
      Files.createDirectory(Paths.get(arguments(FAILED)))
    }

    // TODO: Add argument parsing for search class path
    val targetName = if (StringUtils.isNotBlank(arguments.getOrElse(TARGET_CLASS, ""))) {
      arguments.get(TARGET_CLASS)
    } else {

      val scanner = new ClassPathScanningCandidateComponentProvider(true)
      scanner.addIncludeFilter(new AssignableTypeFilter(classOf[FuzzTest]))

      // Not sure why the implicit conversation is not kicking in here but this works.
      asScalaSet(scanner.findCandidateComponents(arguments(TARGET_PATH))).map((bd : BeanDefinition) => {
        println(bd.getBeanClassName)
        bd.getBeanClassName
      }).head
    }

    fuzzLoop(targetName.toString)
  }

  private var arguments : Map[String, String] = _

  private lazy val rand = new Random()

  private def fuzzLoop(targetName : String): Unit = {
    val coverageSet = new mutable.HashSet[String]()

    // TODO: Threads
    // TODO: Stats counting and logging.

    val workStack = new mutable.ArrayStack[Array[Byte]]()
    workStack.push(Array[Byte]())
    readCorpusInputStack(arguments(CORPUS), workStack)

    def loop() {

      if (workStack.isEmpty) {
        readCorpusInputStack(arguments(CORPUS), workStack)
      }

      val old = workStack.pop()

      val newInput = mutate(old)
      println("input: " + DatatypeConverter.printHexBinary(newInput))
      val (result, hash, ex) = runOnce(targetName, newInput)
      if (!coverageSet.contains(hash)) {
        coverageSet.add(hash)
        workStack.push(newInput)

        val md5 = MessageDigest.getInstance("MD5")
        val filename = DatatypeConverter.printHexBinary(md5.digest(newInput))

        IOUtils.write(newInput, new FileOutputStream(s"${arguments(CORPUS)}/$filename.input"))

        if (!result) { // failed
          IOUtils.write(newInput, new FileOutputStream(s"${arguments(FAILED)}/$filename.failed"))
          ex.foreach(e => {
            val exOut = new PrintStream(new FileOutputStream(s"${arguments(FAILED)}/$filename.stacktrace"))
            e.printStackTrace(exOut)
            exOut.flush()
            exOut.close()
          })
        }
      }

      loop()
    }
    loop()
  }

  def readCorpusInputStack(path : String, stack : mutable.ArrayStack[Array[Byte]]) : Unit = {
    Files.newDirectoryStream(Paths.get(path)).forEach { f =>
      stack.push(IOUtils.toByteArray(new FileInputStream(f.toFile)))
    }
  }

  private def mutate(input : Array[Byte]) : Array[Byte] = {
    // TODO: Pull this out into a better place
    // TODO: an empty array is valid
    if (input.isEmpty) {
      Array[Byte](0x00) // if we started with empty input make a single byte we can mutate more next time.
    } else { // todo : Many more mutation ideas
      rand.nextInt(100) match { // Make extending the array much less likely than changing existing values
        case 0 => extendArray(input)
        case 1 => doubleUpArray(input)
        case _ => mutateInPlace(input)
      }
    }
  }

  private def mutateInPlace(input : Array[Byte]) : Array[Byte] = {
    rand.nextInt(4) match {
      case 0 => // add one to a random byte
        val index = rand.nextInt(input.length)
        input.update(index, (input(index) + 1).asInstanceOf[Byte])
        input
      case 1 => // Swap a random byte with another randomly generated byte
        changeOneByte(input)
      case 2 => // Bitshift right random byte
        val index = rand.nextInt(input.length)
        input.update(index, (input(index) >> 1).asInstanceOf[Byte])
        input
      case 3 => // Bitshift left random byte
        val index = rand.nextInt(input.length)
        input.update(index, (input(index) << 1).asInstanceOf[Byte])
        input
      case _ => // default change a byte at random
        println("Unknown mutation option.")
        changeOneByte(input)
    }
  }

  private def changeOneByte(input : Array[Byte]) : Array[Byte] = {
    val index = rand.nextInt(input.length)
    val value = rand.nextInt(255).asInstanceOf[Byte]
    input.update(index, value)
    input
  }


  private def extendArray(input : Array[Byte]) : Array[Byte] = {
    val extra = new Array[Byte](rand.nextInt(10))
    rand.nextBytes(extra)
    val result = new Array[Byte](input.length + extra.length)
    input.copyToArray(result, 0)
    extra.copyToArray(extra, input.length)
    result
  }

  private def doubleUpArray(input : Array[Byte]) : Array[Byte] = {
    val result = new Array[Byte](input.length * 2)
    input.copyToArray(result, 0)
    input.copyToArray(result, input.length)
    result
  }


  private def runOnce(targetName : String, input : Array[Byte]) : (Boolean, String, Option[Throwable]) = {

    val memoryClassLoader = new CoverageMemoryClassLoader()
    memoryClassLoader.addClass(targetName)

    val targetClass = memoryClassLoader.loadClass(targetName)

    var result = true

    // Here we execute our test target class through its interface
    val targetInstance = targetClass.newInstance.asInstanceOf[FuzzTest]
    try {
      result = targetInstance.test(input)
      (result, memoryClassLoader.generateCoverageHash(), None)
    } catch {
      case e : Throwable =>
        printf("error thrown from test environment ")
        e.printStackTrace()
        result = false
        (result, memoryClassLoader.generateCoverageHash(), Some(e))
    }


  }


}

