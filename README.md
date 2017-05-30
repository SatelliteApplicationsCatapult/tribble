# Tribble

A fuzz testing tool for java applications using coverage to guide the process. 
Heavily based on the wonderful GoFuzz and AFL.

It uses Jacoco to get coverage stats and has a maven plug for running.

## Usage

### Maven
* Include tribble-core as a dependency. At the moment you'll need to run `mvn install` from this project until I get time
to put tribble into maven central.
```
<dependency>
    <groupId>org.catapult.sa</groupId>
    <artifactId>tribble-core</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```
* Add the plugin to your pom.
```
<plugin>
    <groupId>org.catapult.sa</groupId>
    <artifactId>tribble-maven-plugin</artifactId>
    <version>0.1-SNAPSHOT</version>
    <configuration>
        <target>org.catapult.sa.testcase.TestCase</target>
    </configuration>
</plugin>
```
* Create a class that implements `FuzzTest` which will run a single test case with the provided data. If this class returns false
or throws an exception the run is considered to be a failure.
* Configure the target class name in the plugin.
* Run `mvn tribble:fuzztest` to start a run.

### Command Line
* Include this library in your project.
* Create a class that implements `FuzzTest` which will run a single test case with the provided data. If this class returns false
or throws an exception the run is considered to be a failure.
* Run `org.catapult.sa.tribble.App` passing `--targetClass` to set where your implementation of `FuzzTest` lives and the
required class path.

## Why?

Because we needed one, you probably do too. If you don't think you do you don't have enough tests yet.

### What is fuzz testing?

Fuzz testing (or smoke testing) is the process of sending "random" data (we'll come back to this in a bit) into a system 
and seeing what happens. The process of doing this has been around for a long time. However most of the time the input is
generated from known input and has no way of knowing what happened inside the system under test. Systems like AFL and 
GoFuzz are able to instrument the system and know when they have found new code paths.  This enables them to further modify 
the input data to work their way through the application.

Using systems like this is more effective and efficient that just sending random data into the system. Unfortunately such
systems either don't work with Java/the JVM or have to start the application externally. With the JVM this introduces a 
significant slowdown as the optimisations and compilations from previous runs are lost. 
 
The GoFuzz tool allows the creation of a simple test function which is used as the basis of the test. Tribble used a 
similar approach and is based heavily on the work of GoFuzz and AFL, although not yet as advanced. This allows the tests 
to be run with out restarting the JVM, which makes things a lot faster. However it does mean that if you find a JVM 
crashing bug you will lose the node. We can recover from most out of memory errors. We use Jacoco to generate code 
coverage stats for a run, which lets us work out the paths through the application we have found.

### Why the name Tribble?

Lots of small fuzzy creatures. It seemed like a reasonable name when I started this.

"Letting engineers name things is like letting the marketing department build them" 

## Where next?

* Multi node (Clustering, boss/worker client server system)
* Smarter mutation strategies (See AFL and GoFuzz)
* Find minimum version of input that will produce same branch before saving
* Support Scala 2.11.7 as a minimum (Spark support)
* Gradle support
