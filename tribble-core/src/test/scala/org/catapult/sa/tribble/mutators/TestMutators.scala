package org.catapult.sa.tribble.mutators

import org.junit.Assert._
import org.junit.Test

import org.catapult.sa.tribble.TestUtils._

import scala.util.Random

/**
  * Basic tests of mutators.
  *
  * Still not too nice with the mocking of random however this is a lot better than the old basic mutation engine.
  */
class TestMutators {

  @Test
  def extendShortZero() : Unit = {
    runTest(rand((10, 0)), new ExtendShort(), a(0x01), a(0x01))
  }

  @Test
  def extendShortOne() : Unit = {
    runTest(rand((10, 1)), new ExtendShort(), a(0x01), a(0x01, 0x00))
  }

  @Test
  def extendLongZero() : Unit = {
    runTest(rand((100, 0)), new ExtendLong(), a(0x01), a(0x01))
  }

  @Test
  def extendLongOne() : Unit = {
    runTest(rand((100, 1)), new ExtendLong(), a(0x01), a(0x01, 0x00))
  }

  @Test
  def doubleUp() : Unit = {
    runTest(rand(), new DoubleUp(), a(0x01, 0x16), a(0x01, 0x16, 0x01, 0x16))
  }

  @Test
  def trimShort() : Unit = {
    runTest(rand(), new Trim(), a(0x01), Array.emptyByteArray)
  }

  @Test
  def trimZero() : Unit = {
    runTest(rand(), new Trim(), Array.emptyByteArray, Array.emptyByteArray)
  }

  @Test
  def trimSomething() : Unit = {
    runTest(rand((1, 1)), new Trim(), a(0x01, 0x03, 0x04), a(0x01, 0x03))
  }

  @Test
  def prefix() : Unit = {
    runTest(rand((5, 2)), new Prefix(), a(0x01, 0x03, 0x04), a(0x00, 0x00, 0x01, 0x01, 0x03, 0x04))
  }

  @Test
  def addOne() : Unit = {
    runTest(rand((3, 1)), new AddOne(), a(0x01, 0x03, 0x04), a(0x01, 0x04, 0x04))
  }

  @Test
  def minusOne() : Unit = {
    runTest(rand((3, 1)), new MinusOne(), a(0x01, 0x03, 0x04), a(0x01, 0x02, 0x04))
  }

  @Test
  def changeByte() : Unit = {
    runTest(rand((3, 1), (255, 255)), new ChangeByte(), a(0x01, 0x03, 0x04), a(0x01, 0xFF, 0x04))
  }

  @Test
  def changeFirstByte() : Unit = {
    runTest(rand((255, 255)), new ChangeFirstByte(), a(0x01, 0x03, 0x04), a(0xFF, 0x03, 0x04))
  }

  @Test
  def shiftRight() : Unit = {
    runTest(rand((3, 1)), new ShiftRight(), a(0x01, 0x03, 0x04), a(0x01, 0x01, 0x04))
  }

  @Test
  def shiftLeft() : Unit = {
    runTest(rand((3, 1)), new ShiftLeft(), a(0x01, 0x03, 0x04), a(0x01, 0x06, 0x04))
  }

  @Test
  def flipByte() : Unit = {
    runTest(rand((3, 1)), new FlipByte(), a(0x01, 0x03, 0x04), a(0x01, 0xFC, 0x04))
  }

  @Test
  def swap() : Unit = {
    runTest(rand((3, 1), (3, 2)), new Swap(), a(0x01, 0x03, 0x04), a(0x01, 0x04, 0x03))
  }

  @Test
  def interestingByte() : Unit = {
    runTest(rand((3, 1), (InterestingByte.values.length, 2)), new InterestingByte(), a(0x01, 0x03, 0x04), a(0x01, 0xF0, 0x04))
  }

  @Test
  def interestingIntsShort() : Unit = {
    runTest(rand((3, 1), (InterestingInts.values.length, 1)), new InterestingInts(), a(0x01, 0x03, 0x04), a(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
  }

  @Test
  def interestingInts() : Unit = {
    runTest(rand((3, 1), (InterestingInts.values.length, 2)), new InterestingInts(), a(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07), a(0x01, 0x80, 0x00, 0x00, 0x00, 0x06, 0x07))
  }

  private def runTest(r : Random, m : Mutator, in : Array[Byte], expected : Array[Byte]) : Unit = {
    val result = m.mutate(in, r)

    assertArrayEquals(expected, result)
  }
}
