package org.catapult.sa

import org.catapult.sa.tribble.{CoverageMemoryClassLoader, FuzzTest}
import org.jacoco.core.analysis.{Analyzer, CoverageBuilder, ICounter}
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.runtime.{LoggerRuntime, RuntimeData}
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.`type`.filter.AssignableTypeFilter

/**
 * Hello world! with calculated coverage
 *
 */
object App  {

  def main(args : Array[String]) : Unit = {

    val scanner = new ClassPathScanningCandidateComponentProvider(true)
    scanner.addIncludeFilter(new AssignableTypeFilter(classOf[FuzzTest]))

    scanner.findCandidateComponents("org.catapult.sa").forEach(bd => {

      println(bd.getBeanClassName)

      val targetName = bd.getBeanClassName

      val runtime = new LoggerRuntime

      val data = new RuntimeData()
      runtime.startup(data)

      val memoryClassLoader = new CoverageMemoryClassLoader(runtime)
      memoryClassLoader.addClass(targetName)
      //memoryClassLoader.addFilter("scala.")

      val targetClass = memoryClassLoader.loadClass(targetName)

      // Here we execute our test target class through its interface
      val targetInstance = targetClass.newInstance.asInstanceOf[FuzzTest]
      targetInstance.test(Array[Byte](0x00, 0x00))

      import org.jacoco.core.data.SessionInfoStore
      val executionData = new ExecutionDataStore
      val sessionInfos = new SessionInfoStore
      data.collect(executionData, sessionInfos, false)
      runtime.shutdown()

      val coverageBuilder = new CoverageBuilder
      val analyzer = new Analyzer(executionData, coverageBuilder)

      memoryClassLoader.getDefinedClasses.foreach(e => {
        // have to reload the classes here as we need the un-instrumented ones.
        analyzer.analyzeClass(CoverageMemoryClassLoader.getClassStream(e._1), e._1)
      })

      coverageBuilder.getClasses.forEach(cc => {
        printf("Coverage of class %s%n", cc.getName)
        printCounter("instructions", cc.getInstructionCounter)
        printCounter("branches", cc.getBranchCounter)
        printCounter("lines", cc.getLineCounter)
        printCounter("methods", cc.getMethodCounter)
        printCounter("complexity", cc.getComplexityCounter)

        (cc.getFirstLine to cc.getLastLine).foreach(i => {
          printf("%s:%d : %s\n", cc.getName, i, convertCover(cc.getLine(i).getStatus))
        })
      })

    })
  }

  private def printCounter(name : String, counter : ICounter) : Unit = {
    val missed = counter.getMissedCount
    val total = counter.getTotalCount

    printf("%d of %d %s missed\n", missed, total, name)
  }

  private def convertCover(status : Int) : String = status match {
    case ICounter.EMPTY => "Empty"
    case ICounter.NOT_COVERED => "Not Covered"
    case ICounter.PARTLY_COVERED => "Partial cover"
    case ICounter.FULLY_COVERED => "Fully Covered"
  }

}

