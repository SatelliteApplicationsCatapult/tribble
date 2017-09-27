package org.catapult.sa.tribble

/**
  * Code for handling changes to an input.
  *
  * This is an interface to allow us to experiment easily with different mutation strategies.
  */
trait MutationEngine {
  def mutate(input: Array[Byte]): (Array[Byte], String)
  // TODO: Add some method of feeding back how a mutation went.
}




