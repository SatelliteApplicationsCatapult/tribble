# Release Process

This is mainly documented for my own sake. However it might leave some breadcrumbs for some one else.
 

* Get all your code changes committed to master and in a fit state to release.
* Update the version number in build.gradle to remove the `-SNAPSHOT` extension.
* Create a pull request to get this into master.
* Process the pull request through
* Check out master locally
* Create a release in Github with the name `v{current.version}`
* Run the bintrayUpload task on the root project on your local machine. You will need to have `systemProp.BINTRAY_USER` 
and `systemProp.BINTRAY_KEY` set in your local user gradle.properties DO NOT CHECK THIS IN
* Log in to bintray and publish the two releases
* Bump the version number and put the `-SNAPSHOT` extension back on. 
* Bump the versions in the `README.md` file.
* Create a pull request to update master with the new version number.
 