package org.catapult.sa.tribble

import java.io.{File, FileInputStream, FileOutputStream, PrintStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.MessageDigest
import java.util.concurrent.BlockingQueue
import javax.xml.bind.DatatypeConverter

import org.apache.commons.io.IOUtils

/**
  * Functions for dealing with the corpus of inputs and mutating them.
  */
object Corpus {

  def validateDirectories(corpusPath : String, failedPath : String): Boolean = {
    val corpusFile = new File(corpusPath)
    if (corpusFile.exists() && (!corpusFile.isDirectory || !corpusFile.canWrite)) {
      println("ERROR: corpus path exists but is not a directory: " + corpusFile.getAbsolutePath)
      return false
    }

    if (!corpusFile.exists()) {
      println("Corpus folder does not exist. Creating. WARNING you should give the tribbles some guidance where to start.")
      Files.createDirectory(Paths.get(corpusPath))
    }

    val files = corpusFile.listFiles()
    if (files == null || files.isEmpty) {
      println("Corpus folder exists but is empty. WARNING You must provide an initial entry")
      return false
    }

    val failedFile = new File(failedPath)
    if (failedFile.exists() && (!failedFile.isDirectory || !failedFile.canWrite)) {
      println("ERROR: failed path exists but is not a directory: " + failedFile.getAbsolutePath)
      return false
    }

    if (!failedFile.exists()) {
      println("Failed directory does not exist. Creating.")
      Files.createDirectory(Paths.get(failedPath))
    }

    true
  }

  private val lock = new Object()

  def readCorpusInputStack(corpusPath : String, stack: BlockingQueue[Array[Byte]]): Unit = {
    lock.synchronized {
      if (stack.isEmpty) { // don't write it twice.
        Files.newDirectoryStream(Paths.get(corpusPath)).forEach { f =>
          val stream = new FileInputStream(f.toFile)
          if (f.getFileName.endsWith(".hex")) {
            val hexString = IOUtils.toString(stream, StandardCharsets.UTF_8)
            stack.put(DatatypeConverter.parseHexBinary(hexString))
          } else {
            stack.put(IOUtils.toByteArray(stream))
          }
          IOUtils.closeQuietly(stream)
        }
      }
    }
  }

  def saveResult(input: Array[Byte], success: Boolean, ex: Option[Throwable], corpusPath : String, failedPath : String): Unit = {

    val md5 = MessageDigest.getInstance("MD5")
    val filename = if(!success) {
      ex match {
        case None => "NoStackTrace"
        case Some(e) => createExceptionFileName(e)
      }
    } else if (input == null) {
      "null"
    } else {
      DatatypeConverter.printHexBinary(md5.digest(input))
    }

    if (!success) { // failed, record it in the crashers. But don't keep it for mutations.
      Corpus.saveArray(input, s"$failedPath/$filename.failed")
      ex.foreach(e => {
        val exOut = new PrintStream(new FileOutputStream(s"$failedPath/$filename.stacktrace"))
        e.printStackTrace(exOut)
        exOut.flush()
        exOut.close()
      })
    } else { // new and didn't fail so add it to our corpus
      Corpus.saveArray(input, s"$corpusPath/$filename.input")
    }
  }

  private def createExceptionFileName(t : Throwable) : String = {
    val in = t.getStackTrace.map(_.toString).mkString("\n")
    val md5 = MessageDigest.getInstance("MD5")
    DatatypeConverter.printHexBinary(md5.digest(in.getBytes(StandardCharsets.UTF_8)))
  }

  def saveArray(input: Array[Byte], fileName: String): Unit = {
    if (containsUnprintableChars(input)) {
      if (!new File(s"$fileName.hex").exists()) {
        val stream = new FileOutputStream(s"$fileName.hex")
        IOUtils.write(DatatypeConverter.printHexBinary(input), stream, StandardCharsets.UTF_8)
        IOUtils.closeQuietly(stream)
      }
    } else {
      if (!new File(fileName).exists()) {
        val stream = new FileOutputStream(fileName)
        IOUtils.write(input, stream)
        IOUtils.closeQuietly(stream)
      }
    }
  }

  // This should handle other unprintable characters. (Really we should deal with multibyte stuff and so on)
  // Though on the other hand we can probably deal with other lower order characters easily
  // We should probably keep non hex files to actually type-able stuff
  private def containsUnprintableChars(input: Array[Byte]): Boolean = {
    if (input == null) {
      false
    } else {
      input.exists(b => b < 0x20 || b > 0x7F)
    }
  }


}
