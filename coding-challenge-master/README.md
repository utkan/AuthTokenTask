# coding-challenge

Welcome to our Android coding challenge 👋.

In this challenge, you are asked to build some reactive code that provides an Observable of an auth token. 
The requirements are defined by a set of unit tests. 

The basic gist is: Try to minimise the network requests that are being performed and prevent outdated tokens from being emitted.


## What we expect you to do

Show us how experienced you are with rxjava and make as many of the unit tests we provided pass.


## The setup

* Checkout the code and import it into an IDE
* In case you are having compile errors from Android Studio, try running `File > Sync Project With Gradle Files`
* You can run the unit tests with ```./gradlew test``` whenever you need.
* There is basically just 3 files provided:
    * ```AuthTokenProvider```: The class that needs to be implemented
    * ```AuthToken```: A simple data class for the AuthToken
    * ```AuthTokenProviderTest```: The unit tests you should pass


## The rules

* Please do not change ```AuthToken``` or ```AuthTokenProviderTest```!
* Keep the signature of the `AuthTokenProvider` constructor and `AuthTokenProvider.observeToken()` function as they are.
* You should build your implementation within `AuthTokenProvider`. If you need more classes or functions for your implementation, go ahead and create them.
* We pass a few arguments in the ```AuthTokenProvider``` - please make use of them, so that the unit tests have a chance to work.
* ```AuthToken``` has a few helper functions that you might need.

## The answer

* We should be able to run the project.
* You can send your answer in one of the following ways:
    * Zip it and send us via email.
    * Zip it and put in a remote storage.
    * Send us as a repo we can access.

