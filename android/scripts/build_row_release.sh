./gradlew clean

./gradlew :android_framework:patch-magisk:module:zipRelease --no-daemon

./gradlew app:assembleRowRelease --no-daemon
