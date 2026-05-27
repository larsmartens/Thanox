./gradlew clean

./gradlew :android_framework:patch-magisk:module:zipRelease --no-daemon

./gradlew app:assemblePrcRelease --no-daemon
