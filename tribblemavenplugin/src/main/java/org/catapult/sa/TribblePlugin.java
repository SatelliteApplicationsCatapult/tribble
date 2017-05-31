package org.catapult.sa;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.catapult.sa.tribble.App;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;

import java.io.File;
import java.net.MalformedURLException;

/**
 *
 */
@Mojo(name = "fuzztest",
        defaultPhase = LifecyclePhase.TEST,
        requiresDependencyCollection = ResolutionScope.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class TribblePlugin extends AbstractMojo {

    @Parameter(property = "fuzztest.target", required = true)
    public String target;


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
                if (!(a.getGroupId().equals("org.catapult.sa") && a.getArtifactId().equals("tribble-core"))) {
                    projectRealm.addURL(a.getFile().toURI().toURL());
                }
            }

            Thread.currentThread().setContextClassLoader(projectRealm);

            String[] args = {"--targetClass", target};

            App.setArgs(args);
            App.fuzzWithThreads(target, projectRealm);
        } catch (DuplicateRealmException | MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
