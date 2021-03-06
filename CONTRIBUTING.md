# How to Contribute

Any contributions no matter how small are always welcome. We welcome every thing from bug reports, typo corrections in 
documentation to new features or translations. We try to label issues that should be reasonably easy to do and give a 
good introduction as 
[Help Wanted](https://github.com/SatelliteApplicationsCatapult/tribble/issues?q=is%3Aissue+is%3Aopen+label%3A%22Help+Wanted%22) 
However don't feel limited to tackling these. If you want to jump in to something else go for it.

## Short version
Gradle build. Github fork, branch, and pull requests. Github issues. Travis CI. Bintray JCenter releases.

If you didn't understand that; fantastic! We really want your help, if you don't understand something below please raise
a [github issue](https://github.com/SatelliteApplicationsCatapult/tribble/issues/new). We will do our best to clarify and
improve this document. 

## Getting started

### You will need: 
* A [github](https://github.com/) account

If you are planning to make code changes you will also need: 

* A [Git client](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) on your machine
* The [JDK](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html) installed 
* Clone the repo `git clone git@github.com:SatelliteApplicationsCatapult/tribble.git`
 
## Reporting a bug:
Click the [issues](https://github.com/SatelliteApplicationsCatapult/tribble/issues) link at the top of the project page 
on git hub. Click the bright [New issue](https://github.com/SatelliteApplicationsCatapult/tribble/issues/new) button. 

Please give as much detail as you can. Stack traces, example code, system and platform details. Every thing no matter how
trivial may help tracking down whats going on in your case. Don't worry too much. Just give as much detail as you can.

## Making a documentation change

Open the file on Github. Eg the readme.md file. In the corner there should be a little pen icon. This will open up a 
pretty good text editor. See the [help](https://help.github.com/categories/writing-on-github/) for more details. When 
you are done with your changes there is a place to leave a message about what you have changed, finally click the commit 
changes button. 

There should now be a green bar near the top of the page asking if you want to create a 
[pull request](https://help.github.com/articles/about-pull-requests/).

## Making a code change

Using your local clone, make the change on a branch. If it is a large change please raise an issue first. So we know 
what you are working on and don't duplicate effort. 

When your ready:
* Fork the repo to your own user
* Check that the change builds ( `./gradlew build` )
* Check there are some tests (Yes there are not many so far but we don't want the test debt to get worse)
* Check the code style is consistent (Do not change the indenting because your editor decided to)
* Check the file permissions haven't changed. (Cygwin and windows can be evil for this)
* Create a pull request.

We will then review the changes and get back to you with any questions, or hopefully just approve it.

## Building for different scala versions

If you need a version of this library with a different scala dependency. You should only need to change the 
`ext.scalaVersion` value in the `build.gradle` file and rebuild. Note we support as a base 2.11.7 so something will show
 deprecated warnings in 2.12. Please make sure any changes to the code build against 2.11.7. 
