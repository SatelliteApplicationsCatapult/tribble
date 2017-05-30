package org.catapult.sa.tribble

import java.io.{File, FileInputStream, FileOutputStream, PrintStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.MessageDigest
import java.util.concurrent.BlockingQueue
import javax.xml.bind.DatatypeConverter

import org.apache.commons.io.IOUtils

import scala.util.Random

/**
  * Functions for dealing with the corpus of inputs and mutating them.
  */
object Corpus {

  val CORPUS = "corpus"
  val FAILED = "failed"

  def validateDirectories(arguments: Map[String, String]): Boolean = {
    val corpusFile = new File(arguments(Corpus.CORPUS))
    if (corpusFile.exists() && (!corpusFile.isDirectory || !corpusFile.canWrite)) {
      println("ERROR: corpus path exists but is not a directory: " + corpusFile.getAbsolutePath)
      return false
    }

    if (!corpusFile.exists()) {
      println("Corpus folder does not exist. Creating. WARNING you should give the tribbles some guidance where to start.")
      Files.createDirectory(Paths.get(arguments(Corpus.CORPUS)))
    }

    val files = corpusFile.listFiles()
    if (files == null || files.isEmpty) {
      println("Corpus folder exists but is empty. WARNING You must provide an initial entry")
      return false
    }

    val failedFile = new File(arguments(Corpus.FAILED))
    if (failedFile.exists() && (!failedFile.isDirectory || !failedFile.canWrite)) {
      println("ERROR: failed path exists but is not a directory: " + failedFile.getAbsolutePath)
      return false
    }

    if (!failedFile.exists()) {
      println("Failed directory does not exist. Creating.")
      Files.createDirectory(Paths.get(arguments(Corpus.FAILED)))
    }

    true
  }

  private val lock = new Object()

  def readCorpusInputStack(arguments: Map[String, String], stack: BlockingQueue[Array[Byte]]): Unit = {
    lock.synchronized {
      if (stack.isEmpty) { // don't write it twice.
        Files.newDirectoryStream(Paths.get(arguments(CORPUS))).forEach { f =>
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

  def saveResult(input: Array[Byte], success: Boolean, ex: Option[Throwable], arguments: Map[String, String]): Unit = {

    val md5 = MessageDigest.getInstance("MD5")
    val filename = if(input == null) "null" else DatatypeConverter.printHexBinary(md5.digest(input))

    if (!success) { // failed, record it in the crashers. But don't keep it for mutations.
      Corpus.saveArray(input, s"${arguments(FAILED)}/$filename.failed")
      ex.foreach(e => {
        val exOut = new PrintStream(new FileOutputStream(s"${arguments(FAILED)}/$filename.stacktrace"))
        e.printStackTrace(exOut)
        exOut.flush()
        exOut.close()
      })
    } else { // new and didn't fail so add it to our corpus
      Corpus.saveArray(input, s"${arguments(CORPUS)}/$filename.input")
    }
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
  private def containsUnprintableChars(input: Array[Byte]): Boolean = input.exists(b => b < 0x20 || b > 0x7F)

  def mutate(input: Array[Byte], rand: Random): Array[Byte] = {
    // TODO: an empty array is valid
    if (input.isEmpty) {
      Array[Byte](0x00) // if we started with empty input make a single byte we can mutate more next time.
    } else { // todo : Many more mutation ideas
      rand.nextInt(100) match { // Make extending the array much less likely than changing existing values
        case 0 => extendArray(input, rand)
        case 1 => doubleUpArray(input, rand)
        case 2 => extendArray(input, rand, 100) // big extend
        case 3 => if (input.length > 4) {
          trim(input, rand)
        } else {
          mutate(input, rand) // try again.
        }
        case _ => mutateInPlace(input, rand)
      }
    }
  }

  private def mutateInPlace(input: Array[Byte], rand: Random): Array[Byte] = {
    val index = rand.nextInt(input.length)
    rand.nextInt(10) match {
      case 0 => // Add one to a random byte
        input.update(index, b(input(index) + 1))
        input
      case 1 => // Swap a random byte with another randomly generated byte
        changeOneByte(input, rand)
      case 2 => // Bit shift right random byte
        input.update(index, b(input(index) >> 1))
        input
      case 3 => // Bit shift left random byte
        input.update(index, b(input(index) << 1))
        input
      case 4 => // Minus one to a random byte
        input.update(index, b(input(index) - 1))
        input
      case 5 => // Shuffle two random bytes
        val index2 = rand.nextInt(input.length)
        if (index != index2) {
          val v1 = input(index)
          input.update(index, input(index2))
          input.update(index2, v1)
          input
        } else {
          // Unlikely unless very short array. Recurse and try again. Random numbers it will exit at some point unless
          // the rng is really broken. (yes this is not in position 4 in case of the use of the XKCD rng.)
          mutateInPlace(input, rand)
        }
      case 6 => // Replace bytes with random entried from list of known interesting values
        val entry = interestingBytes(rand.nextInt(interestingBytes.length))
        input.update(index, entry)
        input
      case 7 => // Replace a block of bytes with an interesting set of bytes.
        if (index + 3 < input.length) {
          val entry = interestingInts(rand.nextInt(interestingInts.length))
          input.update(index, b((entry & 0xFF000000) >> 24))
          input.update(index + 1, b((entry & 0xFF0000) >> 16))
          input.update(index + 2, b((entry & 0xFF00) >> 8))
          input.update(index + 3, b(entry & 0xFF))

          input
        } else { // Too close to the end so try something else.
          mutateInPlace(input, rand) // try again.
        }
      case 8 => // Flip all the bits in a byte.
        input.update(index, b(input(index) ^ 0xFF))
        input
      case 9 => // Flip all the bits in a four byte block
        if (index + 3 < input.length) {
          input.update(index, b(input(index) ^ 0xFF))
          input.update(index+1, b(input(index+1) ^ 0xFF))
          input.update(index+2, b(input(index+2) ^ 0xFF))
          input.update(index+3, b(input(index+3) ^ 0xFF))
          input
        } else { // Too close to the end so try something else.
          mutateInPlace(input, rand) // try again.
        }
      case _ => // default change a byte at random
        println("Unknown mutation option.")
        changeOneByte(input, rand)
    }
  }

  private lazy val interestingBytes = List[Byte](
    b(0x00),
    b(0xFF),
    b(0xF0),
    b(0x0F),
    b(0x01),
    b(0x10),
    b(0xCC),
    b('a'),
    b('z'),
    b('A'),
    b('Z'),
    b('0'),
    b('9'),
    b('('),
    b(')'),
    b('<'),
    b('>'),
    b('{'),
    b('}'),
    b('['),
    b(']'),
    b(';'),
    b(' '),
    b('-'),
    b('!')
  )

  private lazy val interestingInts = List[Int](
    Int.MaxValue,
    0,
    Int.MinValue,
    Int.MaxValue - 1,
    Int.MinValue + 1,
    -1,
    1,
    16,
    32,
    64,
    128,
    256,
    512,
    1024,
    2048
  )

  private def trim(input: Array[Byte], rand: Random): Array[Byte] = {
    val shortenBy = rand.nextInt(input.length / 2) // don't chop more than half the array off.
    val result = new Array[Byte](input.length - shortenBy)
    Array.copy(input, 0, result, 0, input.length - shortenBy)
    result
  }

  private def changeOneByte(input: Array[Byte], rand: Random): Array[Byte] = {
    val index = rand.nextInt(input.length)
    val value = b(rand.nextInt(255))
    input.update(index, value)
    input
  }


  private def extendArray(input: Array[Byte], rand: Random, length: Int = 10): Array[Byte] = {
    val extra = new Array[Byte](rand.nextInt(length))
    rand.nextBytes(extra)
    val result = new Array[Byte](input.length + extra.length)
    input.copyToArray(result, 0)
    extra.copyToArray(extra, input.length)
    result
  }

  private def doubleUpArray(input: Array[Byte], rand: Random): Array[Byte] = {
    val result = new Array[Byte](input.length * 2)
    input.copyToArray(result, 0)
    input.copyToArray(result, input.length)
    result
  }

  // Short cut to convert to byte.
  private def b(i: Int): Byte = i.asInstanceOf[Byte]

}
