#!/bin/bash
set -e

./mvnw spotless:apply
./mvnw clean verify 2>&1 | tee build.log
