#!/usr/bin/env sh
# Gradle Wrapper Launcher für das Repository
gradle wrapper --gradle-version 8.13 --distribution-type bin
exec gradle "$@"
