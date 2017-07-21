package org.catapult.sa.tribble.mutators

import scala.util.Random

import org.catapult.sa.tribble.ByteUtils._

/**
  * This file contains the default set of mutators.
  *
  * If you add an entry in here make sure it is inlcuded in
  * tribble\tribble-core\src\main\resources\META-INF\org.catapult.sa.tribble.mutators
  *
  * TODO: add a gradle build step to generate the file from this file. "class ([A-Z][a-zA-Z0-9]+) extends Mutator {"
  */

/**
  * Definition of a thing that can handle a mutation.
  */
trait Mutator {
  def mutate(in : Array[Byte], rand : Random) : Array[Byte]
}

class ExtendShort extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val extra = new Array[Byte](rand.nextInt(10))
    rand.nextBytes(extra)
    Array.concat(in, extra)
  }
}

class ExtendLong extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val extra = new Array[Byte](rand.nextInt(100))
    rand.nextBytes(extra)
    Array.concat(in, extra)
  }
}

class DoubleUp extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    Array.concat(in, in)
  }
}

class Trim extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    if (in.length <= 1) {
      Array.emptyByteArray
    } else {
      val to = in.length - rand.nextInt(in.length / 2) // don't chop more than half the array off.
      val result = new Array[Byte](to)
      Array.copy(in, 0, result, 0, to)
      result
    }
  }
}

class Prefix extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val newPrefix = Prefix.byteMarkers(rand.nextInt(Prefix.byteMarkers.length))
    Array.concat(newPrefix, in)
  }
}

object Prefix {
  val byteMarkers : List[Array[Byte]] = List[Array[Byte]] (
    Array(b(0x00), b(0x00), b(0x00), b(0x0C), b(0x6A), b(0x50), b(0x20), b(0x20), b(0x0D), b(0x0A)), // jpeg2000
    Array(b(0x00), b(0x00), b(0x00), b(0x18), b(0x66), b(0x74), b(0x79), b(0x70), b(0x33), b(0x67), b(0x70), b(0x35)), // MOV
    Array(b(0x00), b(0x00), b(0x01)), // ICO (Not including the half byte B)
    Array(b(0x00), b(0x01), b(0x00), b(0x00), b(0x00)), // True Type font.
    Array(b(0x01), b(0xFF), b(0x02), b(0x04), b(0x03), b(0x02))  // DRW
    // TODO: add many many more of these.
  )
}

class AddOne extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val index = rand.nextInt(in.length)
    in.update(index, b(in(index) + 1))
    in
  }
}

class MinusOne extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val index = rand.nextInt(in.length)
    in.update(index, b(in(index) - 1))
    in
  }
}


class ChangeByte extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val index = rand.nextInt(in.length)
    val value = b(rand.nextInt(255))
    in.update(index, value)
    in
  }
}

class ChangeFirstByte extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val value = b(rand.nextInt(255))
    in.update(0, value)
    in
  }
}

class ShiftRight extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val index = rand.nextInt(in.length)
    in.update(index, b(in(index) >> 1))
    in
  }
}


class ShiftLeft extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val index = rand.nextInt(in.length)
    in.update(index, b(in(index) << 1))
    in
  }
}

class FlipByte extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val index = rand.nextInt(in.length)
    in.update(index, b(in(index) ^ 0xFF))
    in
  }
}

// swap two bytes. If it happens to pick the same byte for both sides this won't do any thing.
class Swap extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val index1 = rand.nextInt(in.length)
    val index2 = rand.nextInt(in.length)

    val v1 = in(index1)

    in.update(index1, in(index2))
    in.update(index2, v1)

    in
  }
}

class InterestingByte extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val index = rand.nextInt(in.length)
    val entry = InterestingByte.values(rand.nextInt(InterestingByte.values.length))

    in.update(index, entry)
    in
  }
}

object InterestingByte {
  val values : List[Byte] = List[Byte](
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
    b('!'),
    b('\r'),
    b('\n'),
    b('\t'),
    b(','),
    b('*'),
    b('\''),
    b('"'),
    b('\\')
  )

}

class InterestingInts extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val work = if (in.length <= 4) {
      val extra = new Array[Byte](4)
      rand.nextBytes(extra)
      Array.concat(in, extra)
    } else {
      in
    }

    val index = rand.nextInt(work.length - 4)
    val entry = InterestingInts.values(rand.nextInt(InterestingInts.values.length))

    work.update(index, b((entry & 0xFF000000) >> 24))
    work.update(index + 1, b((entry & 0xFF0000) >> 16))
    work.update(index + 2, b((entry & 0xFF00) >> 8))
    work.update(index + 3, b(entry & 0xFF))

    work
  }

}

object InterestingInts {
  val values : List[Int] = List[Int](
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
}


