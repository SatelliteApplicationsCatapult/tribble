package org.catapult.sa.tribble

import org.junit.Assert._
import org.junit.Test
import org.mockito.Mockito._

import scala.util.Random

/**
  * Tests of mutation. Mostly using a mock random number generator to force code paths we need.
  *
  * This is a little bit evil. The other option is to set the seed value for a random number generator to give the paths
  * we need. I'm not sure I like that any more.
  */
class TestCorpus {

  @Test
  def checkEmptyInput() : Unit = {
    val rand = mock(classOf[Random])
    val result = Corpus.mutate(Array.emptyByteArray, rand)

    assertArrayEquals(Array[Byte](0x00), result)
  }

  @Test
  def checkSingleByte() : Unit = {
    val rand = mock(classOf[Random])
    when(rand.nextInt(100)).thenReturn(66)
    when(rand.nextInt(1)).thenReturn(0)
    when(rand.nextInt(10)).thenReturn(0)


    val input = Array[Byte](0x00)
    val result = Corpus.mutate(input, rand)

    assertArrayEquals(Array[Byte](0x01), result)
  }

}
