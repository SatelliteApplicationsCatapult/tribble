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

package org.catapult.sa.testcase

/**
  * Part of test environment. Don't ask
  */
object Fish {

  def wibble(data : Array[Byte]): Unit = {
    println("Hello world")
    if (data != null && data.length > 20 ) {
      if (data(19) >= 75) { // should be pretty rare.
        throw new Exception("BANG")
      }
      val i = data(15) | data(16) << 8 | data(17) << 16 | data(18) << 24
      val a = Array.fill(i) ("Hello World")
      println("length : " + a.map(_.length).sum)
    }
  }

}
