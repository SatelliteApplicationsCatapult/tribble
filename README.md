# Tribble

Experiments with fuzz testing java applications using coverage to guide the process. Heavily based on GoFuzz and AFL

Uses Jacoco to get coverage stats.

## Usage

* Include this library in your project.
* Create a class that implements `FuzzTest` which will run a single test case with the provided data. A return of false
or and exception thrown from this class is considered to be a failure.
* Run `org.catapult.sa.tribble.App` passing `--targetClass` to set where your implementation of `FuzzTest` lives


## Why?

Because we needed one, you probably do too. If you don't think you do you don't have enough tests yet.

### What is fuzz testing?

Fuzz testing is the process of sending "random" data (We'll come back to this in a bit) into a system and seeing what 
happens. The process of doing this has been around for a long time. However most of the time the input is generated from
known input and has no way of knowing what happened inside the system under test. Systems like AFL and GoFuzz are able
to instrument the system and know when they have found new code paths this enables them to further modify the input data
to work their way through the application.

Using systems like this is more effective and efficient that just sending random data into the system. Unfortunately such
systems either don't work with Java/the JVM or have to start the application externally. With the JVM this introduces a 
 significant slowdown as the optimisations and compilations from previous runs are lost. 
 
The GoFuzz tool allows the creation of a simple test function which is used as the basis of the test. Tribble used a 
similar approach and is based heavily on the work of GoFuzz and AFL (Though not yet as advanced) This allows the tests 
to be run with out restarting the JVM, which makes things a lot faster. However it does mean that if you find a JVM 
crashing bug you will lose the node. We use Jacoco to generate code coverage stats for a run. This lets us work out the
paths through the application we have found. 

## Where next?

* Multi node (Clustering, boss/worker client server system)
* Smarter mutation strategies (See AFL and GoFuzz)
* Find minimum version of input that will produce same branch before saving.
* Maven plugin to make running easy
* Support Scala 2.11.7 as a minimum (Spark support)
