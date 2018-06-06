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

package org.catapult.sa;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.catapult.sa.tribble.Fuzzer;
import org.catapult.sa.tribble.FuzzerFactory;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;

import java.io.File;
import java.net.MalformedURLException;

/**
 * Simple maven plugin to run a fuzz test.
 */
@Mojo(name = "fuzztest",
        defaultPhase = LifecyclePhase.TEST,
        requiresDependencyCollection = ResolutionScope.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class TribblePlugin extends AbstractMojo {

    @Parameter(property = "fuzztest.target", required = true)
    public String target;

    @Parameter(property = "fuzztest.corpus", required = true, defaultValue = "corpus")
    public String corpusPath;

    @Parameter(property = "fuzztest.failed", required = true, defaultValue = "failed")
    public String failedPath;

    @Parameter(property = "fuzztest.threads", required = true, defaultValue = "2")
    public int threads;

    @Parameter(property = "fuzztest.timeout", required = true, defaultValue = "1000")
    public long timeout;

    @Parameter(property = "fuzztest.count", required = true, defaultValue = "-1")
    public long count;

    @Parameter(property = "fuzztest.verbose", required = true, defaultValue = "true")
    public boolean verbose;

    @Parameter(property = "fuzztest.disabledMutators")
    public String[] disabledMutators;

    @Parameter(property = "fuzztest.ignoreClasses")
    public String[] ignoreClasses;


    @Parameter( defaultValue="${project}", required = true, readonly = true)
    public MavenProject project;

    public void execute() throws MojoExecutionException
    {
        try {
            ClassLoader current = this.getClass().getClassLoader();
            ClassRealm realm;
            if (current != null && current instanceof ClassRealm) {
                realm = (ClassRealm) current;
            } else {
                ClassWorld world = new ClassWorld();
                realm = world.newRealm("plugin.tribble.container", this.getClass().getClassLoader());
            }
            ClassRealm projectRealm = realm.createChildRealm("theproject");

            String outputDir =  project.getBuild().getOutputDirectory();
            String testDir = project.getBuild().getTestOutputDirectory();
            projectRealm.addURL(new File(outputDir).toURI().toURL());
            projectRealm.addURL(new File(testDir).toURI().toURL());

            for (Artifact a: project.getArtifacts()) {
                // don't add tribble again. Its already there via our own dependencies. Weird things happen if not.
                if (!(a.getGroupId().equals("org.catapult.sa") && a.getArtifactId().startsWith("tribble-core"))) {
                    projectRealm.addURL(a.getFile().toURI().toURL());
                }
            }

            Thread.currentThread().setContextClassLoader(projectRealm);

            Fuzzer fuzzer = new FuzzerFactory()
                .defaultStats(verbose)
                .fileSystemCorpus(corpusPath, failedPath)
                .ignore(ignoreClasses)
                .threads(threads)
                .timeout(timeout)
                .iterations(count)
                .disable(disabledMutators)
                .build();

            fuzzer.run(target, projectRealm);

        } catch (DuplicateRealmException | MalformedURLException e) {
            throw new MojoExecutionException("Could not run fuzz test. Class path problems.", e);
        }
    }
}
