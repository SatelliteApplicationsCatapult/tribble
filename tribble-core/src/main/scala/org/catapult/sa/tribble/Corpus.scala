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

import java.io.{File, FileInputStream, FileOutputStream, PrintStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest
import java.util.concurrent.BlockingQueue
import javax.xml.bind.DatatypeConverter

import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Functions for dealing with the corpus of inputs and outputs
  */
abstract class Corpus {
  def validate() : Boolean

  def readCorpus(stack: BlockingQueue[(Array[Byte], String)]): Unit

  def saveResult(input: Array[Byte], success: Boolean, ex: Option[Throwable]) : Unit
}

/**
  * File system based corpus.
  * @param corpusPath the directory path to the corpus
  * @param failedPath the directory path where to put the failed files.
  */
class FileSystemCorpus(corpusPath : String, failedPath : String) extends Corpus {
  override def validate(): Boolean = {
    val corpusFile = new File(corpusPath)
    if (corpusFile.exists() && (!corpusFile.isDirectory || !corpusFile.canWrite)) {
      println("ERROR: corpus path exists but is not a directory or not writable: " + corpusFile.getAbsolutePath)
      return false
    }

    if (!corpusFile.exists()) {
      println("Corpus folder does not exist. Creating. WARNING: you should give the tribbles some guidance where to start.")
      Files.createDirectory(Paths.get(corpusPath))
    }

    val files = corpusFile.listFiles()
    if (files == null || files.isEmpty) {
      println("Corpus folder exists but is empty. WARNING: Creating a default empty file. You should give the tribbles some guidance where to start.")
      createEmptyFile(corpusPath + "/default.txt")
    }

    val failedFile = new File(failedPath)
    if (failedFile.exists() && (!failedFile.isDirectory || !failedFile.canWrite)) {
      println("ERROR: failed path exists but is not a directory or writable: " + failedFile.getAbsolutePath)
      return false
    }

    if (!failedFile.exists()) {
      println("Failed directory does not exist. Creating.")
      Files.createDirectory(Paths.get(failedPath))
    }

    true
  }

  private val lock = new Object()

  override def readCorpus(stack: BlockingQueue[(Array[Byte], String)]): Unit = {
    if (stack.isEmpty) {
      lock.synchronized {
        if (stack.isEmpty) {
          if (corpus.isEmpty) {
            loadInputs(corpusPath)
          }

          corpus.foreach(stack.add)
        }
      }
    }
  }

  private def loadInputs(corpusPath : String ) : Unit = {
    corpus.appendAll(
      Files.newDirectoryStream(Paths.get(corpusPath)).asScala
        .map((f: Path) => readCorpusFile(f) -> "corpus")
    )
  }

  private val corpus : mutable.ArrayBuffer[(Array[Byte], String)] = mutable.ArrayBuffer.empty


  override def saveResult(input: Array[Byte], success: Boolean, ex: Option[Throwable]): Unit = {

    val filename = if(!success) {
      ex match {
        case None => "NoStackTrace"
        case Some(e) => createExceptionFileName(e)
      }
    } else if (input == null) {
      "null"
    } else {
      val md5 = MessageDigest.getInstance("MD5")
      DatatypeConverter.printHexBinary(md5.digest(input))
    }

    if (!success) { // failed, record it in the crashers. But don't keep it for mutations.
      if (! Files.exists(Paths.get(s"$failedPath/$filename.failed"))) {
        saveArray(input, s"$failedPath/$filename.failed")

        ex.foreach(e => {
          val exOut = new PrintStream(new FileOutputStream(s"$failedPath/$filename.stacktrace"))
          e.printStackTrace(exOut)
          exOut.flush()
          exOut.close()
        })
      }
    } else { // new and didn't fail so add it to our corpus
      saveArray(input, s"$corpusPath/$filename.input")
      corpus.append(input -> "Corpus")
    }
  }

  private def createExceptionFileName(t : Throwable) : String = {
    val in = cleanStackTrace(t)
    val md5 = MessageDigest.getInstance("MD5")
    DatatypeConverter.printHexBinary(md5.digest(in.getBytes(StandardCharsets.UTF_8)))
  }

  private def cleanStackTrace(t : Throwable) : String = {
    val stack = t.getStackTrace.map(_.toString)
    (stack.head.filterNot(Character.isDigit) +: stack.tail).mkString("\n")
  }

  private def saveArray(input: Array[Byte], fileName: String): Unit = {
    val f = new File(fileName)
    if (!f.exists()) {
      val stream = new FileOutputStream(f)
      IOUtils.write(input, stream)
      stream.close()
    }
  }

  // Provide useful options for the corpus so we can add corpus entries easier.
  private def readCorpusFile(f: Path) : Array[Byte] = {
    val stream = new FileInputStream(f.toFile)
    val result = if (f.endsWith(".hex")) {
      val hexString = IOUtils.toString(stream, StandardCharsets.UTF_8)
      DatatypeConverter.parseHexBinary(hexString)
    } else if (f.endsWith(".b64")) {
      val b64String = IOUtils.toString(stream, StandardCharsets.UTF_8)
      DatatypeConverter.parseBase64Binary(b64String)
    } else {
      IOUtils.toByteArray(stream)
    }
    stream.close()
    result
  }

  private def createEmptyFile(name: String) : Unit = {
    val f = new FileOutputStream(name)
    IOUtils.write("", f , StandardCharsets.UTF_8)
    f.close()
  }

}
