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

import org.catapult.sa.tribble.stats.{ConsoleStats, RunDetails, Stats}

class FuzzerFactory {

  private var threadCount : Int = 4
  private var iterationCount : Long = -1
  private var timeoutDuration : Long = 1000L
  private var ignoreList : List[String] = List()
  private var disabledMutators : List[String] = List()
  private var corpusClass : Corpus = _
  private var statsClass : Stats[_ <: RunDetails] = _

  def threads(t  : Int) : FuzzerFactory = {
    threadCount = t
    this
  }

  def iterations(i : Long) : FuzzerFactory = {
    iterationCount = i
    this
  }

  def timeout(t : Long) : FuzzerFactory = {
    timeoutDuration = t
    this
  }

  def ignore(className : String*) : FuzzerFactory = {
    ignoreList = ignoreList ++ className
    this
  }
  // Duplicated so it can be handled from java
  def ignore(className : Array[String]) : FuzzerFactory = {
    ignoreList = ignoreList ++ className
    this
  }


  def disable(mutator : String*) : FuzzerFactory = {
    disabledMutators = disabledMutators ++ mutator
    this
  }
  // Duplicated so it can be handled from java
  def disable(mutator : Array[String]) : FuzzerFactory = {
    disabledMutators = disabledMutators ++ mutator
    this
  }

  // TODO: Add different types of corpus creation here
  def fileSystemCorpus(corpusPath : String, failedPath : String) : FuzzerFactory = {
    corpusClass = new FileSystemCorpus(corpusPath, failedPath)
    this
  }

  // TODO: Add different types of stats creation here
  def defaultStats(details : Boolean) : FuzzerFactory = {
    statsClass = new ConsoleStats(details)
    this
  }

  def build() : Fuzzer = {
    new Fuzzer(corpusClass, statsClass, ignoreList.toArray, threadCount, timeoutDuration, iterationCount, disabledMutators.toArray)
  }


}
