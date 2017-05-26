package org.catapult.sa.tribble

import scala.collection.mutable
import scala.util.Random

/**
  * Generator for a byte array content for a fuzzing run.
  */
class Generator {

}

object Generator {
  // TODO: Thread local
  val r = new Random()

  // TODO: Consider making lazy if possible.
  def combine[A](xs: Traversable[Traversable[A]]): Seq[Seq[A]] =
    xs.foldLeft(Seq(Seq.empty[A])){
      (x, y) => for (a <- x.view; b <- y) yield a :+ b
    }
}

trait Section {
  def length() : Int
  def value(in : Array[Byte], offset : Int)
  def evolve(interesting : Boolean) : List[Section]
}

class GroupValues(val children : List[Section]) extends Section {

  override def length(): Int = children.map(_.length()).sum

  override def value(in: Array[Byte], offset: Int): Unit = {
    var counter = offset
    children.foreach(c => {
      c.value(in, counter)
      counter = counter + c.length()
    })
  }

  override def evolve(interesting: Boolean): List[Section] = {
    val newChildren = children.map(c => {
      c.evolve(interesting)
    })

    Generator.combine(newChildren).map(s => new GroupValues(simplify(s))).toList
  }

  private def simplify(in : Traversable[Section]) : List[Section] = {
    if (in.isEmpty) {
      List()
    } else {
      val r = in.flatMap {
        case c : GroupValues =>
          val s = c.simplify(c.children)

          if (s.isEmpty) {
            None // prune empty groups
          } else if (s.size == 1) {
            Some(s.head) // Flatten single entry groups.
          } else {
            Some(new GroupValues(s)) // replace.
          }
        case c => Some(c)
      }.toList

      val result = mutable.Buffer(r.head)
      var done = false
      var i = 1
      while (!done) {
        if (r(i).isInstanceOf[FixedValue] && result.last.isInstanceOf[FixedValue]) {
          val b = new Array[Byte](r(i).length() + result.last.length())
          result.last.value(b, 0)
          r(i).value(b, result.last.length())

          result.remove(result.length - 1)
          result.append(new FixedValue(b))
        }

        if (r(i).isInstanceOf[RandomValue] && result.last.isInstanceOf[RandomValue]) {
          val l = r(i).length() + result.last.length()

          result.remove(result.length - 1)
          result.append(new RandomValue(l))
        }

        i = i + 1
        if (i >= r.length) {
          done = true
        }
      }
      result.toList
    }
  }
}

class FixedValue(value : Array[Byte]) extends Section {

  override def length(): Int = value.length

  override def value(in: Array[Byte], offset: Int): Unit = value.copyToArray(in, offset)

  override def evolve(interesting: Boolean): List[Section] = {
    if (interesting) {
      // TODO: Blerg here be the hard bit.
      List()
    } else {
      List(new RandomValue(value.length))
    }
  }
}

class RandomValue(private val l : Int) extends Section {

  private val v = new Array[Byte](l)
  Generator.r.nextBytes(v)

  override def length(): Int = l

  override def value(in: Array[Byte], offset: Int): Unit = v.copyToArray(in, offset)

  override def evolve(interesting: Boolean): List[Section] = {
    if (interesting) {
      List(new FixedValue(v))
    } else {
      List(new RandomValue(l))
    }
  }
}

