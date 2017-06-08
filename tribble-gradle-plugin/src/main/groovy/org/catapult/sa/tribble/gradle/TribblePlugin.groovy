package org.catapult.sa.tribble.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class TribblePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply('java')

        project.extensions.add("tribble", TribblePluginExtension)

        project.task("fuzz") {
            dependsOn : project.tasks.find {
                it.name.startsWith("compile")
            }
            doLast {
                project.javaexec {
                    classpath project.sourceSets.test.runtimeClasspath
                    main 'org.catapult.sa.tribble.App'
                    args "--targetClass", project.tribble.target

                    if (project.tribble.corpusPath) {
                        args "--corpus", project.tribble.corpusPath
                    }

                    if (project.tribble.failedPath) {
                        args "--corpus", project.tribble.failedPath
                    }

                    if (project.tribble.threads != 2) {
                        args "--threads", project.tribble.threads
                    }

                    if (project.tribble.timeout != 1000L) {
                        args "--timeout", project.tribble.timeout
                    }

                    if (project.tribble.count != 0L) {
                        args "--count", project.tribble.count
                    }
                }
            }
        }

    }
}


class TribblePluginExtension {
    String  target
    String  corpusPath
    String  failedPath
    int     threads = 2
    long    timeout = 1000L
    long    count = 0L
}