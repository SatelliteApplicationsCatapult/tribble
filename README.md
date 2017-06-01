# Tribble

[![Build Status](https://travis-ci.org/SatelliteApplicationsCatapult/tribble.svg?branch=master)](https://travis-ci.org/SatelliteApplicationsCatapult/tribble)

An easy to use fuzz testing tool for java applications using coverage to guide the process. 
Heavily based on the wonderful GoFuzz and AFL.

It uses Jacoco to get coverage stats and has a maven plug for running.

## Usage

### Maven
* Add the JCenter maven repository to your pom if needed. You will need **both** repository and pluginRepository entries
```Maven POM
<repositories>
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>central</id>
        <name>bintray</name>
        <url>http://jcenter.bintray.com</url>
    </repository>
</repositories>
<pluginRepositories>
    <pluginRepository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>central</id>
        <name>bintray-plugins</name>
        <url>http://jcenter.bintray.com</url>
    </pluginRepository>
</pluginRepositories>
```

* Include tribble-core as a dependency. 
```Maven POM
<dependency>
    <groupId>org.catapult.sa</groupId>
    <artifactId>tribble-core</artifactId>
    <version>0.2</version>
    <scope>test</scope>
</dependency>
```
* Add the plugin to the build section of your pom.
```Maven POM
<plugin>
    <groupId>org.catapult.sa</groupId>
    <artifactId>tribble-maven-plugin</artifactId>
    <version>0.2</version>
    <configuration>
        <target>org.catapult.sa.testcase.TestCase</target>
    </configuration>
</plugin>
```
* Create a class that implements `FuzzTest` which will run a single test case with the provided data. If this class returns false
or throws an exception the run is considered to be a failure. This class can be in the `src/test` tree
* Configure the target class name in the plugin.
* Create a folder called `corpus` and populate it with an initial set of inputs that will exercise different functions in 
your application. The more the merrier.
* Run `mvn tribble:fuzztest` to start a run.

### Command Line
* Include this library in your project.
* Create a class that implements `FuzzTest` which will run a single test case with the provided data. If this class returns false
or throws an exception the run is considered to be a failure.
* Create a folder called `corpus` and populate it with an initial set of inputs that will exercise different functions in 
your application. The more the merrier.
* Run `org.catapult.sa.tribble.App` passing `--targetClass` to set where your implementation of `FuzzTest` lives and the
required class path.

### General Usage
Stats will be printed to stderr at regular intervals. A folder called `failed` will be created on startup and populated 
with both inputs and stack traces that they cause. The file names will be the md5 hash of the input. Inputs which generate
new code paths will be saved to the corpus directory with a file name that is an md5 hash of the code coverage stats.

Try and avoid printing to stdout and stderr in your test cases this can slow things down quite a lot. There is a one second 
time out on each test run. 

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

[Lots](https://en.wikipedia.org/wiki/Tribble) of small fuzzy creatures from Star trek. It seemed like a reasonable name 
when I started this.

"Letting engineers name things is like letting the marketing department build them" 

## "Advanced" settings
There are some extra settings you may wish to use if needed. They are accessable either from the maven plugin or the 
command line.

### Maven
| Tag |  Description | Default |
| --- | --- | --- |
| target | Fully qualified class name of a class which implements `org.catapult.sa.tribble.FuzzTest` | |
| corpusPath | Path to the corpus folder | corpus |
| failedPath | Path to the failed folder | failed |
| threads | Number of threads to use | 2 |
| timeout | Time out for an individual test run in milliseconds | 1000 |

### Command Line

| Parameter | Description | Default |
| --- | --- | --- |
| --targetClass | Fully qualified class name of a class which implements `org.catapult.sa.tribble.FuzzTest` | |
| --corpus | Path to the corpus folder | corpus |
| --failed | Path to the failed folder | failed |
| --threads | Number of threads to use | 2 |
| --timeout | Time out for an individual test run in milliseconds | 1000 |

## Where next?

* Multi node (Clustering, boss/worker client server system)
* Smarter mutation strategies (See AFL and GoFuzz)
* Find minimum version of input that will produce same branch before saving
* Support Scala 2.11.7 as a minimum (Spark support)
* Gradle support

## Contributing 

See the [CONTRIBUTING.md](CONTRIBUTING.md) file and the [Code of conduct](CODE_OF_CONDUCT.md)

## Thanks

This project owes a huge thank you to the [AFL](http://lcamtuf.coredump.cx/afl/) project and [go-fuzz](https://github.com/dvyukov/go-fuzz)
 for the ideas. [JaCoCo](https://github.com/jacoco/jacoco) for the code coverage tool this is built on top of.