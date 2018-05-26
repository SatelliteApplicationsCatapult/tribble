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

package org.catapult.sa.tribble.stats

/**
  * Keep track of the stats. Number of runs, number of crashes etc
  *
  * This is used for displaying a running log as the program is running.
  */
abstract class Stats[T <: RunDetails] {
  def startRun() : T

  def finishRun(start : T) : Unit

  def render() : Unit
}


class RunDetails(var success : Boolean = false, var ignored : Boolean = false, var timeout : Boolean = false, var newPath : Boolean = false, var mutator : String = "")
