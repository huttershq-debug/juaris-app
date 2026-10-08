#!/usr/bin/env sh
# Gradle Wrapper Launcher für das Repository
gradle wrapper --gradle-version 8.5 --distribution-type bin
exec gradle "$@"
